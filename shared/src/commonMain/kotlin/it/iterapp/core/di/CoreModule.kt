package it.iterapp.core.di

import it.iterapp.core.api.IterGatewayClient
import it.iterapp.core.api.iterHttpClient
import it.iterapp.core.offline.OfflineRepository
import it.iterapp.core.repo.PlacesRepository
import it.iterapp.core.repo.PlanRepository
import it.iterapp.core.repo.ReliabilityRepository
import it.iterapp.core.repo.SearchRepository
import it.iterapp.core.repo.TrainsRepository
import it.iterapp.core.settings.IterSettings
import okio.FileSystem
import okio.Path
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The shared-core Koin module. Platforms provide [platformCoreModule] (a
 * `Settings` store, the filesystem and the offline root) and start Koin with
 * `coreModules()`.
 */
expect fun platformCoreModule(): Module

fun coreModule(): Module = module {
  single { IterSettings(get()) }
  single { iterHttpClient() }
  single { IterGatewayClient(get()) { get<IterSettings>().gatewayOrigin.value } }
  single { SearchRepository(get()) }
  single { PlanRepository(get()) }
  single { TrainsRepository(get()) }
  single { PlacesRepository(get()) }
  single { ReliabilityRepository(get()) }
  single { OfflineRepository(get(), get<FileSystem>(), get<Path>(OfflineRootQualifier)) }
}

fun coreModules(): List<Module> = listOf(coreModule(), platformCoreModule())

val OfflineRootQualifier = org.koin.core.qualifier.named("offline-root")
