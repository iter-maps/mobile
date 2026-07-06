package it.iterapp.app.di

import it.iterapp.app.home.HomeViewModel
import it.iterapp.app.location.LocationProvider
import it.iterapp.app.offline.OfflineViewModel
import it.iterapp.app.place.PlaceDetailViewModel
import it.iterapp.app.planning.PlanningViewModel
import it.iterapp.app.search.SearchViewModel
import it.iterapp.app.settings.SettingsViewModel
import it.iterapp.app.trains.TrainBoardViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
  single { LocationProvider(androidContext()) }

  viewModel { HomeViewModel(get(), get()) }
  viewModel { SearchViewModel(get(), get()) }
  viewModel { PlanningViewModel(get(), get()) }
  viewModel { TrainBoardViewModel(get()) }
  viewModel { OfflineViewModel(get()) }
  viewModel { PlaceDetailViewModel(get()) }
  viewModel { SettingsViewModel(get()) }
}
