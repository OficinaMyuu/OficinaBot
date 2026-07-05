package bot

import (
	"log/slog"
	"testing"

	"oficina-registrar/internal/registration"
)

func TestRouterResolve(t *testing.T) {
	t.Parallel()

	router := NewRouter("r!", slog.Default())
	tests := []struct {
		name string
		raw  string
		want Route
		ok   bool
	}{
		{
			name: "ignores non prefix",
			raw:  "hello",
			ok:   false,
		},
		{
			name: "routes register fallback",
			raw:  "r!f18p <@123>",
			want: Route{Args: []string{"f18p", "<@123>"}},
			ok:   true,
		},
		{
			name: "routes revoke",
			raw:  "r!revoke <@123>",
			want: Route{Name: "revoke", Args: []string{"<@123>"}},
			ok:   true,
		},
		{
			name: "preserves old revoke prefix behavior",
			raw:  "r!revokefoo <@123>",
			want: Route{Name: "revoke", Args: []string{"<@123>"}},
			ok:   true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			got, ok := router.Resolve(tt.raw)
			if ok != tt.ok {
				t.Fatalf("Resolve() ok = %v, want %v", ok, tt.ok)
			}
			if !equalRoutes(got, tt.want) {
				t.Fatalf("Resolve() = %#v, want %#v", got, tt.want)
			}
		})
	}
}

func TestMergeMemberRoles(t *testing.T) {
	t.Parallel()

	got := MergeMemberRoles(
		[]string{"kept", registration.RoleNonRegistered.ID(), registration.RoleDesktop.ID(), registration.RoleDesktop.ID()},
		[]registration.Role{registration.RoleRegistered},
		[]registration.Role{registration.RoleNonRegistered},
	)

	want := []string{"kept", registration.RoleDesktop.ID(), registration.RoleRegistered.ID()}
	if len(got) != len(want) {
		t.Fatalf("MergeMemberRoles() = %#v, want %#v", got, want)
	}
	for i := range want {
		if got[i] != want[i] {
			t.Fatalf("MergeMemberRoles() = %#v, want %#v", got, want)
		}
	}
}

func equalRoutes(a Route, b Route) bool {
	if a.Name != b.Name || len(a.Args) != len(b.Args) {
		return false
	}
	for i := range a.Args {
		if a.Args[i] != b.Args[i] {
			return false
		}
	}
	return true
}
