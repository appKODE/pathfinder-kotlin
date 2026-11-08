package ru.kode.pathfinder.android.store

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun SqlDelightStore(context: Context): SqlDelightStore =
  SqlDelightStore(AndroidSqliteDriver(PathFinderDatabase.Schema, context, name = "pathfinder.db"))
