package com.orbital.run.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.orbital.run.ui.theme.AirPrimary
import com.orbital.run.ui.theme.AirTextPrimary
import com.orbital.run.ui.theme.AirTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarminLoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Connexion Garmin",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AirTextPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Entrez vos identifiants Garmin pour synchroniser vos activités.",
                    fontSize = 14.sp,
                    color = AirTextSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = AirTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            if (email.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                // Save password securely for background sync
                                com.orbital.run.logic.Persistence.saveGarminPassword(context, password)
                                onLogin(email, password)
                            }
                        },
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Text("Connexion")
                        }
                    }
                }
            }
        }
    }
}
