package utils

import "testing"

func TestFormatEpochAcceptsSecondsAndMilliseconds(t *testing.T) {
	want := "2023-11-14T22:13:20Z"

	for _, epoch := range []int64{1_700_000_000, 1_700_000_000_123} {
		if got := FormatEpoch(epoch); got != want {
			t.Fatalf("FormatEpoch(%d) = %q, want %q", epoch, got, want)
		}
	}
}
