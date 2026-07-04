# Backend Terraform

This directory owns the OCI infrastructure for the backend service. The root module only wires providers, shared data, common tags, module inputs, and outputs. Resource ownership lives under `modules/`:

- `network/`: VCN, subnets, route tables, security lists, NSGs, and NSG rules.
- `compute/`: API and bot compute instances.
- `mysql/`: private OCI MySQL DB system.
- `load_balancer/`: public IPv4 flexible load balancer, backend set, HTTP/HTTPS listeners, and the HTTPS certificate bundle.

Terraform provisions infrastructure only. Product schema is managed separately by the root `database/` migrator; do not add `local-exec`, provisioners, or application startup DDL to make Terraform apply SQL migrations.

## Local Setup

Create a local backend config from `backend.oci.tfbackend.example`:

```hcl
bucket    = "<oci-object-storage-bucket-name>"
namespace = "<oci-object-storage-namespace>"
region    = "us-ashburn-1"
key       = "terraform/terraform.tfstate"
tenancy_ocid     = "ocid1.tenancy.oc1.."
user_ocid        = "ocid1.user.oc1.."
fingerprint      = "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"
private_key_path = "/.oci/oci_api_key.pem"
```

The backend file repeats OCI API credentials because `terraform init` configures remote state before Terraform evaluates provider variables.

Create a local `terraform.tfvars` from `terraform.tfvars.example`, or provide the same values through `TF_VAR_*` environment variables:

```powershell
$env:TF_VAR_tenancy_ocid = "ocid1.tenancy.oc1.."
$env:TF_VAR_user_ocid = "ocid1.user.oc1.."
$env:TF_VAR_fingerprint = "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"
$env:TF_VAR_private_key_path = "C:/Users/Leonardo/.oci/oci_api_key.pem"
$env:TF_VAR_compartment_id = "ocid1.compartment.oc1.."
$env:TF_VAR_ssh_public_key = "ssh-ed25519 AAAA..."
$env:TF_VAR_ssh_source_cidr = "203.0.113.10/32"
$env:TF_VAR_lb_https_public_certificate = @"
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
"@
$env:TF_VAR_lb_https_private_key = @"
-----BEGIN PRIVATE KEY-----
...
-----END PRIVATE KEY-----
"@
$env:TF_VAR_mysql_admin_username = "admin"
$env:TF_VAR_mysql_admin_password = "<strong-password>"
```

The load balancer HTTPS certificate values come from Cloudflare:

1. Open Cloudflare dashboard for `oficinamyuu.com.br`.
2. Go to `SSL/TLS` -> `Origin Server` -> `Origin Certificates` -> `Create Certificate`.
3. Use Cloudflare-generated key material, PEM format, and include `api.oficinamyuu.com.br` as a hostname.
4. Store the generated certificate as `lb_https_public_certificate`.
5. Store the generated private key as `lb_https_private_key`.

A separate `lb_https_ca_certificate` value is optional for the current OCI load balancer listener. If Cloudflare or OCI later requires an explicit chain for the selected certificate type, provide it through `TF_VAR_lb_https_ca_certificate` or the optional `CLOUDFLARE_ORIGIN_CA_PEM` GitHub secret.

Then run:

```powershell
terraform fmt -recursive
terraform init -backend-config=backend.oci.tfbackend
terraform validate
terraform plan
```

`terraform.tfvars`, `*.tfbackend`, state files, and plan files are ignored. Do not commit them.

## GitHub Actions

The workflow at `.github/workflows/backend-terraform.yml` maps uppercase GitHub secret names into the exact Terraform variable names through `scripts/prepare-github-actions-env.sh`. Uppercase secret names are fine; the important part is that the workflow exports variables such as `TF_VAR_private_key_path`, not `TF_VAR_PRIVATE_KEY_PATH`.

Required repository secrets:

- `OCI_TENANCY_OCID`
- `OCI_USER_OCID`
- `OCI_FINGERPRINT`
- `OCI_PRIVATE_KEY_PEM`
- `OCI_COMPARTMENT_OCID`
- `OCI_OBJECT_STORAGE_NAMESPACE`
- `OCI_TF_STATE_BUCKET`
- `SSH_PUBLIC_KEY`
- `SSH_SOURCE_CIDR`
- `MYSQL_ADMIN_USERNAME`
- `MYSQL_ADMIN_PASSWORD`
- `CLOUDFLARE_ORIGIN_CERT_PEM`
- `CLOUDFLARE_ORIGIN_PRIVATE_KEY_PEM`

Optional repository secret:

- `CLOUDFLARE_ORIGIN_CA_PEM`

Paste `OCI_PRIVATE_KEY_PEM` as the full PEM file contents, including the `BEGIN` and `END` lines. The helper script writes it to `$RUNNER_TEMP/oci_api_key.pem`, locks the permissions down, and points Terraform at that path through `TF_VAR_private_key_path`.

Paste the Cloudflare Origin Certificate into `CLOUDFLARE_ORIGIN_CERT_PEM` and the matching private key into `CLOUDFLARE_ORIGIN_PRIVATE_KEY_PEM`, including the `BEGIN` and `END` lines. These values are passed to Terraform as sensitive variables and installed on the OCI load balancer. Terraform sensitive values are still stored in the remote state, so restrict access to the OCI Object Storage bucket used for Terraform state.

Pull requests run formatting and validation with `terraform init -backend=false`, so they do not need secrets. Pushes to `main` run a real remote-backend plan. Applies are manual through `workflow_dispatch` with `apply=true` and use the `backend-infra` GitHub Environment. The apply job creates a temporary plan file in `$RUNNER_TEMP` and applies that same file inside the protected job; plan files are not committed or uploaded as artifacts.

Configure protection rules on the `backend-infra` environment before using manual apply.

## Cloudflare Protected API

The load balancer is IPv4-only and is intended to be reached through Cloudflare:

```text
User -> Cloudflare -> OCI Load Balancer -> API VM
```

Terraform manages:

- An OCI load balancer HTTPS listener on port 443 using the Cloudflare Origin CA certificate.
- OCI load balancer NSG ingress on ports 80 and 443 from Cloudflare IPv4 ranges only.

Terraform does not manage Cloudflare DNS, WAF/rate limiting, Pages, or zone settings. Keep those resources in the Cloudflare dashboard unless the repository deliberately adopts Cloudflare Terraform ownership later.

Configure Cloudflare manually:

1. `DNS` -> `Records`: create a proxied `A` record for `api.oficinamyuu.com.br` pointing to `load_balancer_public_ip`.
2. `SSL/TLS` -> `Overview`: set SSL/TLS encryption mode to `Full (strict)` after the OCI HTTPS listener is applied.
3. `Security` -> `WAF` -> `Rate limiting rules`: create a rule matching hostname `api.oficinamyuu.com.br` only.
4. Use expression `(lower(http.host) eq "api.oficinamyuu.com.br")`.
5. Set characteristics to source IP and Cloudflare colo, threshold to 100 requests per 10 seconds, mitigation timeout to 10 seconds, and action to `Block`.

The root domain `oficinamyuu.com.br` is expected to be attached to Cloudflare Pages outside this backend Terraform module. The future `cdn.oficinamyuu.com.br` hostname is intentionally not configured here yet.

Terraform outputs `load_balancer_public_ip`, `api_http_url_hint`, and `api_https_url_hint` after apply. Direct public access to the LB IP should fail from non-Cloudflare IPs after the Cloudflare-only NSG rules are applied. Use the Cloudflare hostname for production smoke tests:

```powershell
curl.exe -I https://api.oficinamyuu.com.br/health
curl.exe -I http://<load-balancer-public-ip>/health
```

The first command should return the API health response through Cloudflare. The second command should time out or be blocked from ordinary public networks once the NSG update has applied.

The production path is:

```text
Browser -> Cloudflare -> OCI Load Balancer -> API VM
```

Do not send admin cookies, bearer tokens, OAuth callbacks, or other sensitive traffic through the raw load balancer IP.

## Always Free Guardrails

The configuration is intentionally constrained to OCI Always Free shapes and sizes:

- One API `VM.Standard.E2.1.Micro` compute instance.
- One bots `VM.Standard.A1.Flex` compute instance with 1 OCPU and 6 GB RAM.
- The bots VM has its own `bots_availability_domain_index` so A1 capacity can be targeted separately from the rest of the stack.
- Flexible load balancer fixed at 10 Mbps.
- `MySQL.Free` with 50 GB storage.
- Default 50 GB boot volumes for each compute instance.

Terraform variable validation rejects paid-looking shape, load balancer, MySQL, and boot volume settings.
