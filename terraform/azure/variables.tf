variable "resource_group_name" {
  type        = string
  description = "Where the app reg, user identity"
  # default     = "rg-entrastuff-test"
}

variable "location" {
  type        = string
  description = "Azure region for resources"
  # default     = "North Europe"
}

variable "app_name" {
  type        = string
  description = "Name of the Entra ID app registration"
  # default     = "app-entrastuff-test"
}