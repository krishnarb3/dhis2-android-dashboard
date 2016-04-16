/*
 * Copyright (c) 2015, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.dashboard.ui.activities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.squareup.okhttp.HttpUrl;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.dashboard.R;
import org.hisp.dhis.android.dashboard.api.job.NetworkJob;
import org.hisp.dhis.android.dashboard.api.models.UserAccount;
import org.hisp.dhis.android.dashboard.api.models.meta.Credentials;
import org.hisp.dhis.android.dashboard.api.models.meta.ResponseHolder;
import org.hisp.dhis.android.dashboard.api.persistence.preferences.ResourceType;
import org.hisp.dhis.android.dashboard.utils.AccountUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import fr.castorflex.android.circularprogressbar.CircularProgressBar;

import static org.hisp.dhis.android.dashboard.utils.TextUtils.isEmpty;

public class LoginActivity extends BaseActivity {
    private static final String IS_LOADING = "state:isLoading";
    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    public final static String PARAM_USER_PASS = "USER_PASS";

    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;
    private String mAuthTokenType;
    private AccountManager mAccountManager;

    @Bind(R.id.log_in_views_container)
    View mViewsContainer;

    @Bind(R.id.progress_bar_circular_white)
    CircularProgressBar mProgressBar;

    @Bind(R.id.server_url)
    EditText mServerUrl;

    @Bind(R.id.username)
    EditText mUsername;

    @Bind(R.id.password)
    EditText mPassword;

    @Bind(R.id.log_in_button)
    Button mLogInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        if(getIntent().getStringExtra(AccountUtils.USERNAME)!=null
                && getIntent().getStringExtra(AccountUtils.PASSWORD)!=null) {
            mUsername.setText(getIntent().getStringExtra(AccountUtils.USERNAME));
            mPassword.setText(getIntent().getStringExtra(AccountUtils.PASSWORD));
        } else {
            initAuth();
        }
        hideProgress(false);
        checkEditTextFields();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(IS_LOADING, mProgressBar.isShown());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null &&
                savedInstanceState.getBoolean(IS_LOADING, false)) {
            showProgress(false);
        } else {
            hideProgress(false);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @OnTextChanged(value = {R.id.server_url, R.id.username, R.id.password})
    public void checkEditTextFields() {
        mLogInButton.setEnabled(
                !isEmpty(mServerUrl.getText()) &&
                        (HttpUrl.parse(mServerUrl.getText().toString()) != null) &&
                        !isEmpty(mUsername.getText()) &&
                        !isEmpty(mPassword.getText())
        );
    }

    @OnClick(R.id.log_in_button)
    @SuppressWarnings("unused")
    public void logIn() {
        showProgress(true);

        String serverUrl = mServerUrl.getText().toString();
        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();

        HttpUrl serverUri = HttpUrl.parse(serverUrl);

        getDhisService().logInUser(
                serverUri, new Credentials(username, password)
        );
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onResultReceived(NetworkJob.NetworkJobResult<UserAccount> jobResult) {
        if (ResourceType.USERS.equals(jobResult.getResourceType())) {
            ResponseHolder<UserAccount> responseHolder = jobResult.getResponseHolder();
            if (responseHolder.getApiException() == null) {
                if(getIntent().getStringExtra(AccountUtils.USERNAME)==null
                        && getIntent().getStringExtra(AccountUtils.PASSWORD)==null) {
                    authenticate(responseHolder.getItem().getUId());
                }
                startActivity(new Intent(this, MenuActivity.class));
                finish();
            } else {
                hideProgress(true);
                showApiExceptionMessage(responseHolder.getApiException());
            }
        }
    }

    private void showProgress(boolean withAnimation) {
        if (withAnimation) {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.out_up);
            mViewsContainer.startAnimation(anim);
        }
        mViewsContainer.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress(boolean withAnimation) {
        if (withAnimation) {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.in_down);
            mViewsContainer.startAnimation(anim);
        }
        mViewsContainer.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    private void initAuth() {
        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        mAccountManager = AccountManager.get(getBaseContext());

        mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        if (mAuthTokenType == null)
            mAuthTokenType = AccountUtils.AUTHTOKEN_TYPE_FULL_ACCESS;
    }

    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }

    private void authenticate(String uId) {
        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
        final String userName = mUsername.getText().toString();
        final String passWord = mPassword.getText().toString();
        final String authToken = accountType+"-"+userName+"-"+passWord+"-"+uId;
        Bundle data = new Bundle();
        try {
            data.putString(AccountManager.KEY_ACCOUNT_NAME, userName);
            data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
            data.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            data.putString(PARAM_USER_PASS, passWord);
            Intent result = new Intent();
            result.putExtras(data);
            finishLogin(result);
        } catch (Exception e) {
            data.putString(KEY_ERROR_MESSAGE, e.getMessage());
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void finishLogin(Intent intent) {

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String authtokenType = mAuthTokenType;

            mAccountManager.addAccountExplicitly(account, accountPassword, null);
            mAccountManager.setAuthToken(account, authtokenType, authtoken);
        } else {
            mAccountManager.setPassword(account, accountPassword);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        startActivity(new Intent(LoginActivity.this,MenuActivity.class));
        finish();
    }
}