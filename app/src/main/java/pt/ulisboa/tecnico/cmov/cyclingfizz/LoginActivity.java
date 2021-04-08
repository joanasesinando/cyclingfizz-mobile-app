package pt.ulisboa.tecnico.cmov.cyclingfizz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;


public class LoginActivity extends AppCompatActivity {

    static String TAG = "Cycling_Fizz@Login";
    static int RC_SIGN_IN = 1;

    GoogleSignInClient mGoogleSignInClient;

    private FirebaseAuth mAuth;

    public static boolean isValidEmail(CharSequence target) {
        return Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = FirebaseAuth.getInstance();

        Button signInLater = findViewById(R.id.btn_sign_in_later);
        signInLater.setOnClickListener(v -> {
            finish();
        });

        Button loginBtn = findViewById(R.id.btn_login);

        loginBtn.setOnClickListener(v -> {

            TextInputEditText emailInput =  findViewById(R.id.login_email);
            TextInputLayout emailInputLayout = findViewById(R.id.sign_in_email);

            TextInputEditText passwordInput =  findViewById(R.id.login_password);
            TextInputLayout passwordInputLayout = findViewById(R.id.sign_in_password);


            String email = Objects.requireNonNull(emailInput.getText()).toString();
            String password = Objects.requireNonNull(passwordInput.getText()).toString();

            if (email.isEmpty()) {
                emailInputLayout.setError(getString(R.string.email_required_error));
            } else if (!isValidEmail(email)) {
                emailInputLayout.setError(getString(R.string.email_format_error));
            }

            if (password.isEmpty()) {
                passwordInputLayout.setError(getString(R.string.password_required_error));
            }

            if (!password.isEmpty() && !email.isEmpty() && isValidEmail(email)) {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                // got user
                                assert user != null;
                                Log.d(TAG, "got user " + user.getEmail());
                                finish();
                            } else {
                                // If sign in fails, display a message to the user.
                                Exception e = task.getException();

                                if (e instanceof FirebaseAuthException) {
                                    String errorCode = ((FirebaseAuthException) e).getErrorCode();

                                    switch (errorCode) {
                                        case "ERROR_USER_NOT_FOUND":
                                            emailInput.setError(getString(R.string.email_invalid_error));
                                            break;
                                        case "ERROR_WRONG_PASSWORD":
                                            passwordInput.setError(getString(R.string.password_wrong_error));
                                            break;
                                        default:
                                            emailInput.setError(getString(R.string.auth_failed));
                                            passwordInput.setError(getString(R.string.auth_failed));
                                            Log.e(TAG, errorCode);
                                            break;
                                    }
                                }

                                Toast.makeText(LoginActivity.this, R.string.auth_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(LoginActivity.this, R.string.auth_failed,
                        Toast.LENGTH_SHORT).show();
            }
        });


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        Button loginWithGoogleBtn = findViewById(R.id.btn_google);

        loginWithGoogleBtn.setOnClickListener(v -> {
            signInWithGoogle();
        });

    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                        }
                    }
                });
    }


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null){
            // got user
        }
    }



}