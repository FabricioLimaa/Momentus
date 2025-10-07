package br.com.fabriciolima.momentus.data

import androidx.room.ColumnInfo

/**
 * Uma classe de dados simples (POKO) para armazenar o resultado da consulta de estatísticas.
 * O Room pode preencher objetos deste tipo diretamente a partir de uma consulta SQL complexa.
 */
data class StatsResult(
    @ColumnInfo(name = "nome_rotina")
    val nomeRotina: String,

    @ColumnInfo(name = "cor_rotina")
    val corRotina: String,

    @ColumnInfo(name = "total_minutos")
    val totalMinutos: Long
)
