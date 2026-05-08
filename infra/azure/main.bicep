// Production target topology for live-events-spa.
//
// Documented only. NOT validated against an Azure subscription. The intent
// is to capture how the app would deploy if hooked up to Azure: AKS for the
// API, Event Hubs (Kafka-compatible) for the message bus, Postgres
// Flexible Server for the read-model, Static Web Apps for the React bundle,
// Front Door + WAF in front of the SPA, Application Insights for telemetry.

@description('Deployment region.')
param location string = resourceGroup().location

@description('Short environment slug, e.g. dev / stg / prd.')
param env string = 'dev'

@description('Postgres administrator login.')
param postgresAdmin string

@secure()
@description('Postgres administrator password.')
param postgresAdminPassword string

var prefix = 'lves-${env}'

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: '${prefix}-ai'
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
  }
}

resource eventHubs 'Microsoft.EventHub/namespaces@2024-01-01' = {
  name: '${prefix}-eh'
  location: location
  sku: {
    name: 'Standard'
    tier: 'Standard'
    capacity: 1
  }
  properties: {
    kafkaEnabled: true
  }
}

resource topic 'Microsoft.EventHub/namespaces/eventhubs@2024-01-01' = {
  parent: eventHubs
  name: 'events.live'
  properties: {
    messageRetentionInDays: 1
    partitionCount: 4
  }
}

resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' = {
  name: '${prefix}-pg'
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    version: '16'
    administratorLogin: postgresAdmin
    administratorLoginPassword: postgresAdminPassword
    storage: {
      storageSizeGB: 32
    }
    backup: {
      backupRetentionDays: 7
    }
  }
}

resource aks 'Microsoft.ContainerService/managedClusters@2024-09-01' = {
  name: '${prefix}-aks'
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    dnsPrefix: prefix
    agentPoolProfiles: [
      {
        name: 'system'
        count: 1
        vmSize: 'Standard_B2s'
        mode: 'System'
        osType: 'Linux'
      }
      {
        name: 'user'
        count: 2
        vmSize: 'Standard_B2s'
        mode: 'User'
        osType: 'Linux'
      }
    ]
  }
}

resource staticWeb 'Microsoft.Web/staticSites@2023-12-01' = {
  name: '${prefix}-web'
  location: location
  sku: {
    name: 'Free'
    tier: 'Free'
  }
  properties: {}
}

resource frontDoor 'Microsoft.Cdn/profiles@2024-09-01' = {
  name: '${prefix}-fd'
  location: 'global'
  sku: {
    name: 'Standard_AzureFrontDoor'
  }
}

output appInsightsConnectionString string = appInsights.properties.ConnectionString
output eventHubsHost string = '${eventHubs.name}.servicebus.windows.net:9093'
output postgresFqdn string = postgres.properties.fullyQualifiedDomainName
output staticWebHostname string = staticWeb.properties.defaultHostname
