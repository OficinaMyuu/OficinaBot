#!/usr/bin/env bash
set -euo pipefail

: "${GITHUB_ENV:?GITHUB_ENV is required}"
: "${RUNNER_TEMP:?RUNNER_TEMP is required}"

required=(
  OCI_TENANCY_OCID
  OCI_USER_OCID
  OCI_FINGERPRINT
  OCI_PRIVATE_KEY_PEM
  OCI_COMPARTMENT_OCID
  OCI_OBJECT_STORAGE_NAMESPACE
  OCI_TF_STATE_BUCKET
  SSH_PUBLIC_KEY
  SSH_SOURCE_CIDR
  MYSQL_ADMIN_USERNAME
  MYSQL_ADMIN_PASSWORD
)

missing=0
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "::error::Missing required secret: $name"
    missing=1
  fi
done

if [[ "$missing" -ne 0 ]]; then
  exit 1
fi

add_env() {
  local name="$1"
  local value="$2"
  local delimiter="EOF_${name}_${RANDOM}_${RANDOM}"

  {
    printf '%s<<%s\n' "$name" "$delimiter"
    printf '%s\n' "$value"
    printf '%s\n' "$delimiter"
  } >> "$GITHUB_ENV"
}

region="us-ashburn-1"
state_key="terraform/terraform.tfstate"
private_key_file="$RUNNER_TEMP/oci_api_key.pem"
backend_config_file="$RUNNER_TEMP/backend.oci.tfbackend"

# GitHub stores the OCI private key as a multiline secret. Terraform's OCI
# provider and backend both expect a file path, so each job recreates the file.
printf '%s\n' "$OCI_PRIVATE_KEY_PEM" | sed 's/\r$//' > "$private_key_file"
chmod 600 "$private_key_file"

# Backend configuration is evaluated during `terraform init`, before provider
# variables are available. Keep the generated file in RUNNER_TEMP only.
cat > "$backend_config_file" <<EOF
bucket           = "$OCI_TF_STATE_BUCKET"
namespace        = "$OCI_OBJECT_STORAGE_NAMESPACE"
region           = "$region"
key              = "$state_key"
tenancy_ocid     = "$OCI_TENANCY_OCID"
user_ocid        = "$OCI_USER_OCID"
fingerprint      = "$OCI_FINGERPRINT"
private_key_path = "$private_key_file"
EOF

add_env TF_BACKEND_CONFIG_FILE "$backend_config_file"
add_env TF_VAR_tenancy_ocid "$OCI_TENANCY_OCID"
add_env TF_VAR_user_ocid "$OCI_USER_OCID"
add_env TF_VAR_fingerprint "$OCI_FINGERPRINT"
add_env TF_VAR_private_key_path "$private_key_file"
add_env TF_VAR_region "$region"
add_env TF_VAR_compartment_id "$OCI_COMPARTMENT_OCID"
add_env TF_VAR_ssh_public_key "$SSH_PUBLIC_KEY"
add_env TF_VAR_ssh_source_cidr "$SSH_SOURCE_CIDR"
add_env TF_VAR_mysql_admin_username "$MYSQL_ADMIN_USERNAME"
add_env TF_VAR_mysql_admin_password "$MYSQL_ADMIN_PASSWORD"
