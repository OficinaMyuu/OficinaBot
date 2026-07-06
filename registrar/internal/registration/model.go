package registration

import (
	"errors"
	"fmt"
	"strconv"
	"strings"
	"unicode"
)

type Role string

const (
	RoleRegistered    Role = "REGISTERED"
	RoleNonRegistered Role = "NON_REGISTERED"
	RoleVerifying     Role = "VERIFYING"
	RoleRegistrar     Role = "REGISTRAR"
	RoleAdult         Role = "ADULT"
	RoleTween         Role = "TWEEN"
	RoleUnderage      Role = "UNDERAGE"
	RoleDesktop       Role = "DESKTOP"
	RoleMobile        Role = "MOBILE"
	RoleFemale        Role = "FEMALE"
	RoleMale          Role = "MALE"
	RoleNonBinary     Role = "NON_BINARY"
)

var roleIDs = map[Role]string{
	RoleRegistered:    "664923267601006622",
	RoleNonRegistered: "664921745777623088",
	RoleVerifying:     "758095503845228636",
	RoleRegistrar:     "740360659363168287",
	RoleAdult:         "664918505963126814",
	RoleTween:         "758095500884049960",
	RoleUnderage:      "664918505400958986",
	RoleDesktop:       "664917764229824512",
	RoleMobile:        "664917765395578892",
	RoleFemale:        "664916190082236427",
	RoleMale:          "664916190904320000",
	RoleNonBinary:     "664916189029466122",
}

type Gender string

const (
	GenderFemale    Gender = "FEMALE"
	GenderMale      Gender = "MALE"
	GenderNonBinary Gender = "NON_BINARY"
)

func (g Gender) Role() Role {
	switch g {
	case GenderFemale:
		return RoleFemale
	case GenderMale:
		return RoleMale
	case GenderNonBinary:
		return RoleNonBinary
	default:
		return ""
	}
}

type Device string

const (
	DeviceDesktop Device = "DESKTOP"
	DeviceMobile  Device = "MOBILE"
)

func (d Device) Role() Role {
	switch d {
	case DeviceDesktop:
		return RoleDesktop
	case DeviceMobile:
		return RoleMobile
	default:
		return ""
	}
}

type Action struct {
	Gender Gender
	Device Device
	Age    int
}

var ErrInvalidPattern = errors.New("invalid register pattern")

func ParseAction(pattern string) (Action, error) {
	pattern = strings.TrimSpace(strings.ToLower(pattern))
	if pattern == "" {
		return Action{}, fmt.Errorf("%w: empty pattern", ErrInvalidPattern)
	}

	gender, err := parseGender(pattern[0])
	if err != nil {
		return Action{}, err
	}
	device, err := parseDevice(pattern[len(pattern)-1])
	if err != nil {
		return Action{}, err
	}

	ageText := Digits(pattern)
	age, err := strconv.Atoi(ageText)
	if err != nil {
		return Action{}, fmt.Errorf("%w: invalid age %q", ErrInvalidPattern, ageText)
	}

	return Action{Gender: gender, Device: device, Age: age}, nil
}

func (a Action) HasValidAge() bool {
	return a.Age > 0
}

func (a Action) RolesToAdd() []Role {
	roles := []Role{RoleRegistered, a.Device.Role(), a.Gender.Role()}
	roles = append(roles, RolesByAge(a.Age)...)
	return roles
}

func (a Action) RolesToRemove() []Role {
	return []Role{RoleNonRegistered}
}

func RolesByAge(age int) []Role {
	if age >= 18 {
		return []Role{RoleAdult}
	}
	if age < 13 {
		return []Role{RoleUnderage, RoleTween}
	}
	return []Role{RoleUnderage}
}

func RegisteredRoles() []Role {
	return []Role{
		RoleRegistered,
		RoleAdult,
		RoleTween,
		RoleUnderage,
		RoleDesktop,
		RoleMobile,
		RoleFemale,
		RoleMale,
		RoleNonBinary,
	}
}

func NonRegisteredRoles() []Role {
	return []Role{RoleNonRegistered}
}

func (r Role) ID() string {
	return roleIDs[r]
}

func (r Role) Mention() string {
	return "<@&" + r.ID() + ">"
}

func Digits(input string) string {
	var builder strings.Builder
	for _, char := range input {
		if unicode.IsDigit(char) {
			builder.WriteRune(char)
		}
	}
	return builder.String()
}

func SnowflakeInt64(id string) (int64, error) {
	value, err := strconv.ParseInt(id, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("parse snowflake %q: %w", id, err)
	}
	return value, nil
}

func parseGender(value byte) (Gender, error) {
	switch value {
	case 'f':
		return GenderFemale, nil
	case 'm':
		return GenderMale, nil
	case 'n':
		return GenderNonBinary, nil
	default:
		return "", fmt.Errorf("%w: invalid gender %q", ErrInvalidPattern, value)
	}
}

func parseDevice(value byte) (Device, error) {
	switch value {
	case 'p':
		return DeviceDesktop, nil
	case 'm':
		return DeviceMobile, nil
	default:
		return "", fmt.Errorf("%w: invalid device %q", ErrInvalidPattern, value)
	}
}
