output "client_id" {
  value       = azuread_application.app_entrastuff.client_id
  description = "Client ID of the Entra ID application"
}

output "tenant_id" {
  value       = data.azurerm_subscription.current.tenant_id
  description = "Tenant ID of the Azure subscription"
}

output "client_secret" {
  value       = azuread_application_password.sp_psw_entrastuff.value
  description = "Client secret for the Entra ID application"
  sensitive   = true
}