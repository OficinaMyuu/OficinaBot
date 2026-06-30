# Backend Terraform

This directory owns the OCI infrastructure for the backend service. The root module only wires providers, shared data, common tags, module inputs, and outputs. Resource ownership lives under `modules/`:

- `network/`: VCN, subnets, route tables, security lists, NSGs, and NSG rules.
- `compute/`: API and bot compute instances.
- `mysql/`: private OCI MySQL DB system.
- `load_balancer/`: public IPv4 flexible load balancer, backend set, and HTTP listener.

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
$env:TF_VAR_mysql_admin_username = "admin"
$env:TF_VAR_mysql_admin_password = "<strong-password>"
```

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

Paste `OCI_PRIVATE_KEY_PEM` as the full PEM file contents, including the `BEGIN` and `END` lines. The helper script writes it to `$RUNNER_TEMP/oci_api_key.pem`, locks the permissions down, and points Terraform at that path through `TF_VAR_private_key_path`.

Pull requests run formatting and validation with `terraform init -backend=false`, so they do not need secrets. Pushes to `main` run a real remote-backend plan. Applies are manual through `workflow_dispatch` with `apply=true` and use the `backend-infra` GitHub Environment. The apply job creates a temporary plan file in `$RUNNER_TEMP` and applies that same file inside the protected job; plan files are not committed or uploaded as artifacts.

Configure protection rules on the `backend-infra` environment before using manual apply.

## Public Access

The current load balancer is intentionally HTTP-only and IPv4-only. Terraform outputs `load_balancer_public_ip` and `api_http_url_hint` after apply. Use this public HTTP endpoint only for initial smoke tests such as `/health`; do not send admin cookies, bearer tokens, OAuth callbacks, or other sensitive traffic through public HTTP.

When a domain is ready, the expected production path is Cloudflare in front of OCI:

```text
Browser -> Cloudflare -> OCI Load Balancer -> API VM
```

The later HTTPS work should add:

- A Registro.br domain delegated to Cloudflare nameservers.
- A Cloudflare Pages custom domain for the frontend, for example `oficinamyuu.com.br`.
- A proxied Cloudflare DNS record such as `api.oficinamyuu.com.br` pointing to `load_balancer_public_ip`.
- A Cloudflare Origin CA certificate for `api.oficinamyuu.com.br`.
- A Terraform HTTPS listener, an OCI load balancer certificate resource, and a port 443 NSG rule.
- GitHub secrets for the Origin CA certificate, private key, and CA chain.
- Cloudflare SSL/TLS mode set to Full (strict).
- Optional LB ingress hardening so the load balancer accepts HTTP/HTTPS only from Cloudflare IPv4 ranges.

## Always Free Guardrails

The configuration is intentionally constrained to OCI Always Free shapes and sizes:

- Two `VM.Standard.E2.1.Micro` compute instances.
- Flexible load balancer fixed at 10 Mbps.
- `MySQL.Free` with 50 GB storage.
- Default 50 GB boot volumes for each compute instance.

Terraform variable validation rejects paid-looking shape, load balancer, MySQL, and boot volume settings.
