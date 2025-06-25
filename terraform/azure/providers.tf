terraform {
  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 3.4.0"
    }
    azurerm = {
      source          = "hashicorp/azurerm"
      version         = "~> 4.34.0"
    }
  }
  required_version = ">= 1.0.0"
}

provider "azuread" {
  # Authenticated via Azure CLI (az login)
}

provider "azurerm" {
  subscription_id = "328e0d9b-b770-4def-8e9b-0083bd847e30"
  features {}
  # Authenticated via Azure CLI (az login)
}