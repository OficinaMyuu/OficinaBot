package routes

import (
	"context"
	"errors"
	"net/http"
	"strconv"
	"strings"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/store"
)

type BirthdayRepository interface {
	List(ctx context.Context, filter store.BirthdayFilter) ([]store.Birthday, error)
	Create(ctx context.Context, birthday store.Birthday) (store.Birthday, error)
	Update(ctx context.Context, birthday store.Birthday) (store.Birthday, error)
	Delete(ctx context.Context, userID int64) error
}

type BirthdayHandler struct {
	repository BirthdayRepository
}

type birthdayRequest struct {
	UserID    string `json:"user_id"`
	Name      string `json:"name"`
	Birthday  string `json:"birthday"`
	ZoneHours int    `json:"zone_hours"`
}

type birthdayResponse struct {
	UserID    string `json:"user_id"`
	Name      string `json:"name"`
	Birthday  string `json:"birthday"`
	ZoneHours int    `json:"zone_hours"`
	CreatedAt int64  `json:"created_at"`
	UpdatedAt int64  `json:"updated_at"`
}

func NewBirthdayHandler(repository BirthdayRepository) *BirthdayHandler {
	return &BirthdayHandler{repository: repository}
}

func (h *BirthdayHandler) List(c echo.Context) error {
	month, err := parseOptionalMonth(c.QueryParam("month"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	birthdays, err := h.repository.List(c.Request().Context(), store.BirthdayFilter{
		Search: c.QueryParam("search"),
		Month:  month,
	})
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not list birthdays")
	}

	response := make([]birthdayResponse, 0, len(birthdays))
	for _, birthday := range birthdays {
		response = append(response, toBirthdayResponse(birthday))
	}
	return c.JSON(http.StatusOK, map[string]any{"birthdays": response})
}

func (h *BirthdayHandler) Create(c echo.Context) error {
	birthday, err := bindBirthday(c, "")
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	created, err := h.repository.Create(c.Request().Context(), birthday)
	if err != nil {
		if errors.Is(err, store.ErrDuplicateBirthday) {
			return jsonError(c, http.StatusConflict, "Birthday already exists for this Discord user")
		}
		return jsonError(c, http.StatusInternalServerError, "Could not create birthday")
	}
	return c.JSON(http.StatusCreated, toBirthdayResponse(created))
}

func (h *BirthdayHandler) Update(c echo.Context) error {
	birthday, err := bindBirthday(c, c.Param("userID"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	updated, err := h.repository.Update(c.Request().Context(), birthday)
	if err != nil {
		if errors.Is(err, store.ErrBirthdayNotFound) {
			return jsonError(c, http.StatusNotFound, "Birthday not found")
		}
		return jsonError(c, http.StatusInternalServerError, "Could not update birthday")
	}
	return c.JSON(http.StatusOK, toBirthdayResponse(updated))
}

func (h *BirthdayHandler) Delete(c echo.Context) error {
	userID, err := parseUserID(c.Param("userID"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	if err := h.repository.Delete(c.Request().Context(), userID); err != nil {
		if errors.Is(err, store.ErrBirthdayNotFound) {
			return jsonError(c, http.StatusNotFound, "Birthday not found")
		}
		return jsonError(c, http.StatusInternalServerError, "Could not delete birthday")
	}
	return c.NoContent(http.StatusNoContent)
}

func bindBirthday(c echo.Context, routeUserID string) (store.Birthday, error) {
	var req birthdayRequest
	if err := c.Bind(&req); err != nil {
		return store.Birthday{}, errors.New("Malformed JSON body")
	}

	req.Name = strings.TrimSpace(req.Name)
	req.UserID = strings.TrimSpace(req.UserID)
	if req.UserID == "" {
		return store.Birthday{}, errors.New("Discord user id is required")
	}
	if routeUserID != "" && routeUserID != req.UserID {
		return store.Birthday{}, errors.New("Payload user id must match route user id")
	}
	userID, err := parseUserID(req.UserID)
	if err != nil {
		return store.Birthday{}, err
	}
	if req.Name == "" {
		return store.Birthday{}, errors.New("Name is required")
	}
	if len(req.Name) > 255 {
		return store.Birthday{}, errors.New("Name must be at most 255 characters")
	}
	birthdayDate, err := store.ParseBirthdayDate(req.Birthday)
	if err != nil {
		return store.Birthday{}, err
	}
	if req.ZoneHours < -12 || req.ZoneHours > 14 {
		return store.Birthday{}, errors.New("Timezone offset must be between -12 and 14")
	}

	return store.Birthday{
		UserID:    userID,
		Name:      req.Name,
		Birthday:  birthdayDate,
		ZoneHours: req.ZoneHours,
	}, nil
}

func parseUserID(value string) (int64, error) {
	parsed, err := strconv.ParseInt(value, 10, 64)
	if err != nil || parsed <= 0 {
		return 0, errors.New("Discord user id must be a positive integer")
	}
	return parsed, nil
}

func parseOptionalMonth(value string) (int, error) {
	if value == "" {
		return 0, nil
	}
	month, err := strconv.Atoi(value)
	if err != nil || month < 1 || month > 12 {
		return 0, errors.New("Month must be between 1 and 12")
	}
	return month, nil
}

func toBirthdayResponse(birthday store.Birthday) birthdayResponse {
	return birthdayResponse{
		UserID:    strconv.FormatInt(birthday.UserID, 10),
		Name:      birthday.Name,
		Birthday:  birthday.Birthday.Format(store.DateOnlyLayout),
		ZoneHours: birthday.ZoneHours,
		CreatedAt: birthday.CreatedAt,
		UpdatedAt: birthday.UpdatedAt,
	}
}

type dashboardErrorResponse struct {
	Status  int    `json:"status"`
	Message string `json:"message"`
}

func jsonError(c echo.Context, status int, message string) error {
	return c.JSON(status, dashboardErrorResponse{Status: status, Message: message})
}
