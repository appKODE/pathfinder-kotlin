package ru.kode.pathfinder.android.store

import app.cash.sqldelight.driver.native.NativeSqliteDriver

fun SqlDelightStore(): SqlDelightStore = SqlDelightStore(NativeSqliteDriver(PathFinderDatabase.Schema, "pathfinder.db"))
