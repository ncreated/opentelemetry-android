/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SessionId(sessionId: StateFlow<String>, applicationId: String) {
    val cardColors = CardColors(
        containerColor = Color(0xFFD9D9D9), contentColor = Color(0xFFF5A800),
        disabledContentColor = Color.Black, disabledContainerColor = Color.Black
    )
    Row {
        Card(modifier = Modifier.size(width = 295.dp, height = 100.dp), colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                Arrangement.Center,
            ) {
                CenterText(text = "session.id", fontSize = 13.sp, color = Color.Black)
                //TODO: Fix me -- this selection doesn't work
                SelectionContainer(modifier = Modifier.padding(top = 4.dp)) {
                    CenterText(text = sessionId.collectAsState().value, fontSize = 11.sp,
                        selectable = true, color = Color(0xFF425CC7))
                }
                Spacer(modifier = Modifier.height(6.dp))
                CenterText(text = "application.id", fontSize = 13.sp, color = Color.Black)
                SelectionContainer(modifier = Modifier.padding(top = 4.dp)) {
                    CenterText(text = applicationId, fontSize = 11.sp,
                        selectable = true, color = Color(0xFF425CC7))
                }
            }
        }
    }
}
