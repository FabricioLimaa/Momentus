package br.com.fabriciolima.momentus.data.model

import androidx.room.ColumnInfo

/**
 * Uma classe de dados simples (POKO) para armazenar o resultado da consulta de estatísticas.
 * O Room pode preencher objetos deste tipo diretamente a partir de uma consulta SQL complexa.
 */
data class StatsResult(
    @ColumnInfo(name = "category_name")
    val categoryName: String,

    @ColumnInfo(name = "category_color")
    val categoryColor: String,

    @ColumnInfo(name = "total_minutos")
    val totalMinutos: Long
)
