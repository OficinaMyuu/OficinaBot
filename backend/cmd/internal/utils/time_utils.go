package utils

import "time"

const largestUnixSecondTimestamp = int64(9_999_999_999)

// FormatEpoch converts Unix timestamps stored in either seconds or milliseconds
// into the dashboard's RFC 3339 response format.
func FormatEpoch(epoch int64) string {
	timestamp := time.UnixMilli(epoch)
	if epoch >= -largestUnixSecondTimestamp && epoch <= largestUnixSecondTimestamp {
		timestamp = time.Unix(epoch, 0)
	}
	return timestamp.UTC().Format(time.RFC3339)
}
