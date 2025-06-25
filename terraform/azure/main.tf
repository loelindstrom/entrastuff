# Retrieves the current Azure subscription details, used to output the tenant ID
data "azurerm_subscription" "current" {}

# Fetches well-known application IDs, including Microsoft Graph, for referencing in permissions
data "azuread_application_published_app_ids" "well_known" {}

# Retrieves the Microsoft Graph service principal to access its app role IDs for permissions
data "azuread_service_principal" "msgraph" {
  client_id = data.azuread_application_published_app_ids.well_known.result["MicrosoftGraph"]
}

# Creates an Azure AD application (app registration) with specified display name and audience
resource "azuread_application" "app_entrastuff" {
  display_name     = var.app_name
  sign_in_audience = "AzureADMyOrg" # Specifies the supported account types (e.g., single tenant)

  # Defines the API permissions required by the application (Microsoft Graph in this case)
  required_resource_access {
    resource_app_id = data.azuread_application_published_app_ids.well_known.result["MicrosoftGraph"] # Microsoft Graph API ID

    # Specifies the "User.Read.All" application permission
    resource_access {
      id   = data.azuread_service_principal.msgraph.app_role_ids[local.user_readwrite_all]
      type = "Role" # Indicates an application permission (not delegated)
    }

    # Specifies the "Group.Read.All" application permission
    resource_access {
      id   = data.azuread_service_principal.msgraph.app_role_ids[local.group_readwrite_all]
      type = "Role"
    }
  }
}

# Creates a service principal for the application, enabling it to interact with Azure AD
resource "azuread_service_principal" "sp_entrastuff" {
  client_id                    = azuread_application.app_entrastuff.client_id
  app_role_assignment_required = false # Allows authentication without requiring role assignments
}

# Generates a client secret (password) for the application, used for authentication
resource "azuread_application_password" "sp_psw_entrastuff" {
  application_id = azuread_application.app_entrastuff.id
  display_name   = "sp_psw_entrastuff"
  end_date       = "2025-12-01T09:00:00Z" # Sets expiration date for the secret
}

# Assigns the "User.Read.All" app role from Microsoft Graph to the application's service principal
resource "azuread_app_role_assignment" "user_read_all" {
  principal_object_id = azuread_service_principal.sp_entrastuff.object_id # The application's service principal
  resource_object_id  = data.azuread_service_principal.msgraph.object_id  # Microsoft Graph service principal
  app_role_id         = data.azuread_service_principal.msgraph.app_role_ids[local.user_readwrite_all] # The specific app role
}

# Assigns the "Group.Read.All" app role from Microsoft Graph to the application's service principal
resource "azuread_app_role_assignment" "group_read_all" {
  principal_object_id = azuread_service_principal.sp_entrastuff.object_id
  resource_object_id  = data.azuread_service_principal.msgraph.object_id
  app_role_id         = data.azuread_service_principal.msgraph.app_role_ids[local.group_readwrite_all]
}