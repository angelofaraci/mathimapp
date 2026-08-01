package com.example.proyectofinal.ui

import androidx.compose.runtime.Composable
import com.example.proyectofinal.domain.StudentTrack
import org.jetbrains.compose.resources.stringResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.track_primary
import proyectofinal.composeapp.generated.resources.track_secondary
import proyectofinal.composeapp.generated.resources.track_self_directed
import proyectofinal.composeapp.generated.resources.track_technical_secondary

@Composable
fun StudentTrack.localizedLabel(): String = stringResource(
    when (this) {
        StudentTrack.PRIMARY -> Res.string.track_primary
        StudentTrack.SECONDARY -> Res.string.track_secondary
        StudentTrack.TECHNICAL_SECONDARY -> Res.string.track_technical_secondary
        StudentTrack.SELF_DIRECTED -> Res.string.track_self_directed
    }
)
