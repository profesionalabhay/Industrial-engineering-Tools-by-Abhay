package com.example.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)
    
    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> = Json.decodeFromString(value)

    @TypeConverter
    fun fromDoubleMap(value: Map<String, Double>): String = Json.encodeToString(value)

    @TypeConverter
    fun toDoubleMap(value: String): Map<String, Double> = Json.decodeFromString(value)

    @TypeConverter
    fun fromIntMap(value: Map<String, Int>): String = Json.encodeToString(value)

    @TypeConverter
    fun toIntMap(value: String): Map<String, Int> = Json.decodeFromString(value)

    @TypeConverter
    fun fromVsmNodes(value: List<VsmNode>): String = Json.encodeToString(value)

    @TypeConverter
    fun toVsmNodes(value: String): List<VsmNode> = Json.decodeFromString(value)

    @TypeConverter
    fun fromVsmEdges(value: List<VsmEdge>): String = Json.encodeToString(value)

    @TypeConverter
    fun toVsmEdges(value: String): List<VsmEdge> = Json.decodeFromString(value)

    @TypeConverter
    fun fromSpaghettiNodes(value: List<SpaghettiNode>): String = Json.encodeToString(value)

    @TypeConverter
    fun toSpaghettiNodes(value: String): List<SpaghettiNode> = Json.decodeFromString(value)

    @TypeConverter
    fun fromSpaghettiPaths(value: List<SpaghettiPath>): String = Json.encodeToString(value)

    @TypeConverter
    fun toSpaghettiPaths(value: String): List<SpaghettiPath> = Json.decodeFromString(value)

    @TypeConverter
    fun fromMotionEvents(value: List<MotionEvent>): String = Json.encodeToString(value)

    @TypeConverter
    fun toMotionEvents(value: String): List<MotionEvent> = Json.decodeFromString(value)

    @TypeConverter
    fun fromFiveWhySteps(value: List<FiveWhyStep>): String = Json.encodeToString(value)

    @TypeConverter
    fun toFiveWhySteps(value: String): List<FiveWhyStep> = Json.decodeFromString(value)

    @TypeConverter
    fun fromStringListMap(value: Map<String, List<String>>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringListMap(value: String): Map<String, List<String>> = Json.decodeFromString(value)

    @TypeConverter
    fun fromStandardWorkElements(value: List<StandardWorkElement>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStandardWorkElements(value: String): List<StandardWorkElement> = Json.decodeFromString(value)

    @TypeConverter
    fun fromErgoRiskFactors(value: List<ErgoRiskFactor>): String = Json.encodeToString(value)

    @TypeConverter
    fun toErgoRiskFactors(value: String): List<ErgoRiskFactor> = Json.decodeFromString(value)

    @TypeConverter
    fun fromVsmProcessSteps(value: List<VsmProcessStep>): String = Json.encodeToString(value)

    @TypeConverter
    fun toVsmProcessSteps(value: String): List<VsmProcessStep> = Json.decodeFromString(value)

    @TypeConverter
    fun fromImmutableCandidateSnapshot(value: ImmutableCandidateSnapshot?): String = Json.encodeToString(value)

    @TypeConverter
    fun toImmutableCandidateSnapshot(value: String): ImmutableCandidateSnapshot? = Json.decodeFromString(value)
}
