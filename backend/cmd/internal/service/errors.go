package service

import (
	"fmt"
	"strings"
)

var (
	ErrorInternalServer = NewError(500, "Internal server error")

	ErrorMalformedJSON      = NewError(400, "Malformed JSON body")
	ErrorXpGreaterThanNext  = NewError(400, "Field 'xp' is greater than 'xp_next', which should be impossible")
	ErrorArrayHasNulls      = NewError(400, "Array cannot have nulls")
	ErrorURLNotPresent      = NewError(400, "URL not provided")
	ErrorDuplicateLevelRole = NewError(400, "The array contains 2 or more roles with the same `level` value")
)

type APIError struct {
	Status  int    `json:"status"`
	Message string `json:"message"`
}

func NewError(status int, msg string, args ...any) *APIError {
	if len(args) > 0 {
		msg = fmt.Sprintf(msg, args...)
	}
	return &APIError{Status: status, Message: msg}
}

func ErrorMissingFields(fields ...string) *APIError {
	return NewError(400, "Missing required fields: %s", strings.Join(fields, ", "))
}

func ErrorInvalidArrayLength(length, valid int) *APIError {
	return NewError(400, "Invalid array length; %d elements exceeds the limit of %d values", length, valid)
}

func ErrorValueMustBePositive(fieldName string, value int) *APIError {
	return NewError(400, "Field '%s' should be positive, provided: %d", fieldName, value)
}

func ErrorResponseTooLarge(got, max int64) *APIError {
	return NewError(400, "The response was larger than %d bytes (max), we got %d", max, got)
}

func ErrorDomainNotSupported(domain string) *APIError {
	return NewError(400, "Domain '%s' not supported", domain)
}

func ErrorInvalidValue(fieldName string, provided any, validValues ...any) *APIError {
	return NewError(400, "Invalid value provided for '%s': \"%v\", valid values: (%s)",
		fieldName, provided, humanizeFields(validValues))
}

func ErrorInvalidColor(fieldName string, provided int) *APIError {
	return ErrorInvalidValue(fieldName, provided, fmt.Sprintf("0 - %d", MaxColorValue))
}

func ErrorCannotBeZero(fieldName string, value any) *APIError {
	return NewError(400, "Field '%s' cannot be less than or equal to zero, provided: %v", fieldName, value)
}

func humanizeFields(fields ...any) string {
	var builder strings.Builder
	for i, f := range fields {
		if i > 0 {
			builder.WriteString(", ")
		}
		builder.WriteString(fmt.Sprintf("%v", f))
	}
	return builder.String()
}
