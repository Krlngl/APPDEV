private fun saveData() {
    val name = nameEditText.text.toString().trim()
    val email = emailEditText.text.toString().trim()
    val password = passwordEditText.text.toString().trim()
    val confirmPassword = confirmPasswordEditText.text.toString().trim()

    if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        return
    }

    if (password != confirmPassword) {
        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
        return
    }

    if (selectedImageUri == null) {
        Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
        return
    }

    // Show loading dialog
    loadingDialog.show()

    // Create user with email and password
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    // Upload image to Firebase Storage
                    val storageRef = storage.reference.child("profile_images/${user.uid}")
                    storageRef.putFile(selectedImageUri!!)
                        .addOnSuccessListener { taskSnapshot ->
                            // Get download URL
                            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                // Save user data to Firestore
                                val userData = hashMapOf(
                                    "name" to name,
                                    "email" to email,
                                    "profileImageUrl" to downloadUri.toString()
                                )

                                db.collection("users").document(user.uid)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        // Hide loading dialog
                                        loadingDialog.dismiss()
                                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                        // Navigate to MainActivity
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        loadingDialog.dismiss()
                                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            loadingDialog.dismiss()
                            Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                loadingDialog.dismiss()
                Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
} 