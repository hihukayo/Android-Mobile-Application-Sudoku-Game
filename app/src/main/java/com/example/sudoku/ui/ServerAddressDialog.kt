package com.example.sudoku.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.R
import com.example.sudoku.data.Session

/** 服务器地址设置对话框（登录/注册/设置页共用，文案与 Flutter 版一致） */
@Composable
fun ServerAddressDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val sc = LocalSudokuColors.current
    var value by remember { mutableStateOf(Session.getServerAddress()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.db_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("服务器地址", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column {
                Text("留空则自动连接（USB / 模拟器）", fontSize = 13.sp, color = sc.textFaint)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("例如 192.168.1.100:8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value.trim()) },
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}