package registration

import (
	"errors"
	"reflect"
	"testing"
)

func TestParseAction(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		pattern string
		want    Action
	}{
		{
			name:    "female adult desktop",
			pattern: "f18p",
			want:    Action{Gender: GenderFemale, Device: DeviceDesktop, Age: 18},
		},
		{
			name:    "male mobile with extra digits",
			pattern: "m21m",
			want:    Action{Gender: GenderMale, Device: DeviceMobile, Age: 21},
		},
		{
			name:    "non binary under thirteen",
			pattern: "n12m",
			want:    Action{Gender: GenderNonBinary, Device: DeviceMobile, Age: 12},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			got, err := ParseAction(tt.pattern)
			if err != nil {
				t.Fatalf("ParseAction() error = %v", err)
			}
			if got != tt.want {
				t.Fatalf("ParseAction() = %#v, want %#v", got, tt.want)
			}
		})
	}
}

func TestParseActionRejectsInvalidPatterns(t *testing.T) {
	t.Parallel()

	for _, pattern := range []string{"", "x18p", "f18x", "fpm"} {
		t.Run(pattern, func(t *testing.T) {
			t.Parallel()

			_, err := ParseAction(pattern)
			if !errors.Is(err, ErrInvalidPattern) {
				t.Fatalf("ParseAction() error = %v, want ErrInvalidPattern", err)
			}
		})
	}
}

func TestRolesByAge(t *testing.T) {
	t.Parallel()

	tests := []struct {
		age  int
		want []Role
	}{
		{age: 18, want: []Role{RoleAdult}},
		{age: 17, want: []Role{RoleUnderage}},
		{age: 12, want: []Role{RoleUnderage, RoleTween}},
	}

	for _, tt := range tests {
		got := RolesByAge(tt.age)
		if !reflect.DeepEqual(got, tt.want) {
			t.Fatalf("RolesByAge(%d) = %#v, want %#v", tt.age, got, tt.want)
		}
	}
}

func TestActionRolesToAdd(t *testing.T) {
	t.Parallel()

	action := Action{Gender: GenderFemale, Device: DeviceDesktop, Age: 12}
	want := []Role{RoleRegistered, RoleDesktop, RoleFemale, RoleUnderage, RoleTween}
	if got := action.RolesToAdd(); !reflect.DeepEqual(got, want) {
		t.Fatalf("RolesToAdd() = %#v, want %#v", got, want)
	}
}

func TestDigits(t *testing.T) {
	t.Parallel()

	got := Digits("<@!1234567890>")
	if got != "1234567890" {
		t.Fatalf("Digits() = %q, want %q", got, "1234567890")
	}
}
