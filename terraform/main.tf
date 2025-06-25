# To output the tenant id
data "azurerm_subscription" "current" {}

# Create an Entra ID application
resource "azuread_application" "app_entrastuff" {
  display_name     = var.app_name
  sign_in_audience = "EntraStuff"
}

# Create a service principal for the application
resource "azuread_service_principal" "sp_entrastuff" {
  client_id                    = azuread_application.app_entrastuff.client_id
  app_role_assignment_required = false
}

# Create a client secret for the application
resource "azuread_application_password" "sp_psw_entrastuff" {
  application_id = azuread_application.app_entrastuff.id
  display_name   = "sp_psw_entrastuff"
  end_date       = "2025-12-01T09:00:00Z" # 6 months
}

# Data source to get well-known application IDs (e.g., Microsoft Graph)
data "azuread_application_published_app_ids" "well_known" {}

# Data source to get the Microsoft Graph service principal
data "azuread_service_principal" "msgraph" {
  client_id = data.azuread_application_published_app_ids.well_known.result["MicrosoftGraph"]
}

# Grant Microsoft Graph API permissions
resource "azuread_application_api_access" "graph_access" {
  application_id = azuread_application.app_entrastuff.id
  api_client_id  = data.azuread_application_published_app_ids.well_known.result["MicrosoftGraph"]

  role_ids = [
    data.azuread_service_principal.msgraph.app_role_ids["User.Read.All"],
    data.azuread_service_principal.msgraph.app_role_ids["Group.Read.All"]
  ]
}

