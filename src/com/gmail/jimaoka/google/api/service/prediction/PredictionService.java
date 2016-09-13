package com.gmail.jimaoka.google.api.service.prediction;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.prediction.Prediction;
import com.google.api.services.prediction.Prediction.Builder;
import com.google.api.services.prediction.PredictionScopes;
import com.google.api.services.prediction.model.Input;
import com.google.api.services.prediction.model.Input.InputInput;
import com.google.api.services.prediction.model.Insert;
import com.google.api.services.prediction.model.Insert2;
import com.google.api.services.prediction.model.Output;
import com.google.api.services.storage.StorageScopes;

/**
 * Prediction APIを操作するサービスクラスです
 * @author junji.imaoka
 */
public class PredictionService {
	private static final String APPLICATION_NAME = "HelloPrediction";
	private static final String STORAGE_DATA_LOCATION = "<Google Cloud Storageのバケットのパスを設定>";
	private static final String MODEL_ID = "languageidentifier";
	private static final String PROJECT_ID = "<Google Cloud Platformで作成したプロジェクトID>";
	private static final String SERVICE_ACCT_EMAIL = "<プロジェクトで設定したサービスアカウントのEmail>";
	private static final String SERVICE_ACCT_KEYFILE = "<サービスアカウント作成時に作成した秘密鍵を含むファイル。.p12形式のファイルを使用する>";

	private JsonFactory jsonFactory;
	private HttpTransport httpTransport;

	/**
	 * Googleと認証を行います
	 * @param
	 * @return GoogleCredential
	 * @throws Exception
	 */
	public GoogleCredential authorize() throws Exception{
		GoogleCredential.Builder builder = new GoogleCredential.Builder();
		builder.setTransport(httpTransport);
		builder.setJsonFactory(jsonFactory);
		builder.setServiceAccountId(SERVICE_ACCT_EMAIL);
		builder.setServiceAccountPrivateKeyFromP12File(
				new File(PredictionService.class.getResource(SERVICE_ACCT_KEYFILE).getFile()));
		builder.setServiceAccountScopes(Arrays.asList(PredictionScopes.PREDICTION, StorageScopes.DEVSTORAGE_READ_ONLY));
		return builder.build();
	}

	/**
	 * システムの訓練＆状態のチェック、言語の予測の一連を実行します
	 * @param
	 * @throws Exception
	 */
	public void run() throws Exception{
		jsonFactory = JacksonFactory.getDefaultInstance();
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		GoogleCredential credential = authorize();
		Builder builder = new Prediction.Builder(httpTransport, jsonFactory, credential);
		builder.setApplicationName(APPLICATION_NAME);
		Prediction prediction = builder.build();
		train(prediction);

		predict(prediction, "Qué le gusta hacer cuando no hay un ratón?");
		predict(prediction, "Que voulez-vous faire quand il n'y a pas de souris?");
		predict(prediction, "What would you like to do when there is no mouse?");
	}

	/**
	 * 訓練データでシステムを訓練し、訓練の状態をチェックします
	 * @param prediction
	 * @throws Exception
	 */
	private void train(Prediction prediction) throws IOException{
		Insert trainingData = new Insert();
		trainingData.setId(MODEL_ID);
		trainingData.setStorageDataLocation(STORAGE_DATA_LOCATION);
		prediction.trainedmodels().insert(PROJECT_ID, trainingData);
		System.out.println("Training started.");
		System.out.print("Waiting for training to complete");
		System.out.flush();

		int triesCounter = 0;
		Insert2 trainingModel;
		while(triesCounter < 100){
			try{
				HttpResponse response = prediction.trainedmodels().get(PROJECT_ID, MODEL_ID).executeUnparsed();
				if(response.getStatusCode() == 200){
					trainingModel = response.parseAs(Insert2.class);
					String trainingStatus = trainingModel.getTrainingStatus();
					if(trainingStatus.equals("DONE")){
						System.out.println();
						System.out.println("Training completed.");
						System.out.println(trainingModel.getModelInfo());
						return;
					}
				}
				response.ignore();
			}catch(HttpResponseException e){
			}

			try{
				Thread.sleep(5000 * (triesCounter + 1));
			}catch(InterruptedException e){
				break;
			}
			System.out.print(".");
			System.out.flush();
			triesCounter++;
		}

		error("ERROR:" + "training not completed.");
	}

	/**
	 * 予測を行います
	 * @param prediction
	 * @param text
	 * @return
	 * @throws IOException
	 */
	private void predict(Prediction prediction, String text) throws IOException{
		Input input = new Input();
		InputInput inputInput = new InputInput();
		inputInput.setCsvInstance(Collections.<Object>singletonList(text));
		input.setInput(inputInput);
		Output output = prediction.trainedmodels().predict(PROJECT_ID, MODEL_ID, input).execute();
		System.out.println("Text:" + text);
		System.out.println("Predicted language:" + output.getOutputLabel());
	}

	private void error(String errorMessage){
		System.err.println();
		System.err.println(errorMessage);
		System.exit(1);
	}
}
