package handler

import (
	"context"
	"errors"
	"net/http"
	"strconv"
	"strings"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/contract"
	"oficina-img/internal/domain/entity"
	"oficina-img/internal/domain/mysql/repository"
	"oficina-img/internal/utils"
)

type BirthdayRepository interface {
	List(ctx context.Context, filter repository.BirthdayFilter) ([]entity.Birthday, error)
	Create(ctx context.Context, birthday entity.Birthday) (entity.Birthday, error)
	Update(ctx context.Context, birthday entity.Birthday) (entity.Birthday, error)
	Delete(ctx context.Context, userID int64) error
}

type BirthdayHandler struct {
	repository BirthdayRepository
}

func NewBirthdayHandler(repository BirthdayRepository) *BirthdayHandler {
	return &BirthdayHandler{repository: repository}
}

func (h *BirthdayHandler) List(c echo.Context) error {
	month, err := parseOptionalMonth(c.QueryParam("month"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	birthdays, err := h.repository.List(c.Request().Context(), repository.BirthdayFilter{
		Search: c.QueryParam("search"),
		Month:  month,
	})
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not list birthdays")
	}

	response := make([]contract.BirthdayResponse, 0, len(birthdays))
	for _, birthday := range birthdays {
		response = append(response, toBirthdayResponse(birthday))
	}
	return c.JSON(http.StatusOK, contract.BirthdayListResponse{Birthdays: response})
}

func (h *BirthdayHandler) Create(c echo.Context) error {
	birthday, err := bindBirthday(c, "")
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	created, err := h.repository.Create(c.Request().Context(), birthday)
	if err != nil {
		if errors.Is(err, repository.ErrDuplicateBirthday) {
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
		if errors.Is(err, repository.ErrBirthdayNotFound) {
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
		if errors.Is(err, repository.ErrBirthdayNotFound) {
			return jsonError(c, http.StatusNotFound, "Birthday not found")
		}
		return jsonError(c, http.StatusInternalServerError, "Could not delete birthday")
	}
	return c.NoContent(http.StatusNoContent)
}

func bindBirthday(c echo.Context, routeUserID string) (entity.Birthday, error) {
	var req contract.BirthdayRequest
	if err := c.Bind(&req); err != nil {
		return entity.Birthday{}, errors.New("Malformed JSON body")
	}

	req.Name = strings.TrimSpace(req.Name)
	req.UserID = strings.TrimSpace(req.UserID)
	if req.UserID == "" {
		return entity.Birthday{}, errors.New("Discord user id is required")
	}
	if routeUserID != "" && routeUserID != req.UserID {
		return entity.Birthday{}, errors.New("Payload user id must match route user id")
	}
	userID, err := parseUserID(req.UserID)
	if err != nil {
		return entity.Birthday{}, err
	}
	if req.Name == "" {
		return entity.Birthday{}, errors.New("Name is required")
	}
	if len(req.Name) > 255 {
		return entity.Birthday{}, errors.New("Name must be at most 255 characters")
	}
	birthdayDate, err := repository.ParseBirthdayDate(req.Birthday)
	if err != nil {
		return entity.Birthday{}, err
	}
	if req.ZoneHours < -12 || req.ZoneHours > 14 {
		return entity.Birthday{}, errors.New("Timezone offset must be between -12 and 14")
	}

	return entity.Birthday{
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

func toBirthdayResponse(birthday entity.Birthday) contract.BirthdayResponse {
	return contract.BirthdayResponse{
		UserID:    strconv.FormatInt(birthday.UserID, 10),
		Name:      birthday.Name,
		Birthday:  birthday.Birthday.Format(repository.DateOnlyLayout),
		ZoneHours: birthday.ZoneHours,
		CreatedAt: utils.FormatEpoch(birthday.CreatedAt),
		UpdatedAt: utils.FormatEpoch(birthday.UpdatedAt),
	}
}

func jsonError(c echo.Context, status int, message string) error {
	return c.JSON(status, contract.ErrorResponse{Status: status, Message: message})
}
