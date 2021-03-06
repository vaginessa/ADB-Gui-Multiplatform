package com.yadevapp.adbmpf.presenter;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.yadevapp.adbmpf.model.AndroidApp;
import com.yadevapp.adbmpf.model.AndroidDevice;
import com.yadevapp.adbmpf.model.command.Command;
import com.yadevapp.adbmpf.model.command.CommandManager;
import com.yadevapp.adbmpf.model.command.CommandResponse;
import com.yadevapp.adbmpf.model.command.CommandManager.CommandManagerCallBack;
import com.yadevapp.adbmpf.model.tableModel.DeviceDetailModel;
import com.yadevapp.adbmpf.util.CommandResponseParser;
import com.yadevapp.adbmpf.util.CommandUtil;
import com.yadevapp.adbmpf.util.NetworkUtil;
import com.yadevapp.adbmpf.view.DeviceDetailView;
import com.yadevapp.adbmpf.view.MainView;

public class MainViewPresenter {
	private final String TAG = getClass().getSimpleName();
	private MainView mMainView;
	private CommandManager mCommandManager;
	private List<AndroidDevice> mAndroidDevices;

	public MainViewPresenter(MainView mainView) {
		mMainView = mainView;
		mCommandManager = new CommandManager();
		updateAndroidDeviceList();
	}



	/**
	 * lock the user interface
	 * retreive all connect android devices basic info
	 * and update the device list
	 */
	public void updateAndroidDeviceList() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				mMainView.showProgressDialog("Updating devices list...");
			}
		});
		t.start();
		mAndroidDevices = getConnectedDeviceList();
		//get all apk
		mMainView.onAndroidDeviceListUpdated(mAndroidDevices);
		mMainView.hideProgressDialog();
	}


public void onButtonClicked(ActionEvent e, int[] selectedDeviceRow) {
	List<AndroidDevice> selectedDeviceList = new ArrayList<AndroidDevice>();

	for (int i = 0; i < selectedDeviceRow.length; i++) {
		selectedDeviceList.add(mAndroidDevices.get(selectedDeviceRow[i]));
	}

	System.out.println("onButtonClicked : " + e.getActionCommand());
	String actionCommand = e.getActionCommand();
	FileFilter fileFilter;
	if (actionCommand.equals(MainView.UPDATE_DEVICE_LIST_BUTTON_COMMAND)) {
		updateAndroidDeviceList();
	} else if (actionCommand.equals(MainView.CREATE_BACKUP_BUTTON_COMMAND)) {
		fileFilter = new FileNameExtensionFilter("select restore location", "ab");
		mMainView.showFileChooser(MainView.CREATE_BACKUP_BUTTON_COMMAND, fileFilter, "Create a backup");
	} else if (actionCommand.equals(MainView.RESTORE_BACKUP_BUTTON_COMMAND)) {
		fileFilter = new FileNameExtensionFilter("select an ab file", "ab");
		mMainView.showFileChooser(MainView.RESTORE_BACKUP_BUTTON_COMMAND, fileFilter, "Restore a backup");
	} else if (actionCommand.equals(MainView.INSTALL_APPS_BUTTON_COMMAND)) {
		fileFilter = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}

				String fileName = f.getName().toLowerCase();
				if (fileName.endsWith(".apk")) {
					return true;
				}

				return false; // Reject any other files
			}

			@Override
			public String getDescription() {
				return "apk file or folder containing apks";
			}
		};  
		mMainView.showFileChooser(MainView.INSTALL_APPS_BUTTON_COMMAND, fileFilter, "Install Apps");
	} else if (actionCommand.equals(MainView.SEND_FILE_BUTTON_COMMAND)) {
		fileFilter = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return true;
			}

			@Override
			public String getDescription() {
				return "file or folder to send";
			}
		};  
		mMainView.showFileChooser(MainView.SEND_FILE_BUTTON_COMMAND, fileFilter, "Send Files");
	} else if (actionCommand.equals(MainView.DETAILS_BUTTON_COMMAND)) {
		showDeviceListDetails(selectedDeviceList, "Selected Devices Details");
	} else if (actionCommand.equals(MainView.UNINSTALL_APPS_BUTTON_COMMAND)) {
		showDeviceListDetails(selectedDeviceList, "Uninstall Apps");
	} else if (actionCommand.equalsIgnoreCase(MainView.SOURCE_CODE_COMMAND)) {
		if(Desktop.isDesktopSupported())
		{
			try {
				Desktop.getDesktop().browse(new URI(NetworkUtil.SOURCE_CODE_URL));
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
		}
	}
}

public void onFileChoosed(File choosedFile, int[] selectedRow, String actionCommand) {
	System.out.println("onFileChoosed : " + choosedFile.getAbsolutePath() + " : " + actionCommand);
	List<AndroidDevice> selectedDevice = new ArrayList<AndroidDevice>();

	for(int i = 0; i < selectedRow.length; i++) {
		AndroidDevice androidDevice = mAndroidDevices.get(selectedRow[i]);
		selectedDevice.add(androidDevice);
	}

	if (actionCommand.equals(MainView.CREATE_BACKUP_BUTTON_COMMAND)) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				mMainView.showProgressDialog("Creating backup...");
			}
		});
		t.start();
		createBackup(choosedFile, selectedDevice);
	} else if (actionCommand.equals(MainView.RESTORE_BACKUP_BUTTON_COMMAND)) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				mMainView.showProgressDialog("Restoring backup...");
			}
		});
		t.start();
		restoreBackup(choosedFile, selectedDevice);
		updateAndroidDeviceList();
	} else if (actionCommand.equals(MainView.INSTALL_APPS_BUTTON_COMMAND)) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				mMainView.showProgressDialog("Installing Apps...");
			}
		});
		t.start();
		installApps(choosedFile, selectedDevice);
		updateAndroidDeviceList();
	} else if (actionCommand.equals(MainView.SEND_FILE_BUTTON_COMMAND)) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				mMainView.showProgressDialog("Sending Files...");
			}
		});
		t.start();
		sendFile(choosedFile, selectedDevice);
	} 
	mMainView.hideProgressDialog();

	if (actionCommand.equals(MainView.INSTALL_APPS_BUTTON_COMMAND)
			|| actionCommand.equals(MainView.RESTORE_BACKUP_BUTTON_COMMAND)) {
		updateAndroidDeviceList();
	}
}

public void onSelectedDeviceNumberChanged(int selectedDeviceNumber) {
	mMainView.updateButtonsState(selectedDeviceNumber);
}

public void createBackup(File choosedFile, List<AndroidDevice> selectedDeviceArray) {
	Command command = new Command();
	command.setmCommandString("adb -s " + selectedDeviceArray.get(0).getmId() +  " backup -f " + CommandUtil.QUOTE + choosedFile.getAbsolutePath() + CommandUtil.QUOTE + " -apk -obb -noshared -all -nosystem");
	mCommandManager.addCommandToQueue(command);
	mCommandManager.executeQueue(new CommandManagerCallBack() {

		@Override
		public void onCommandStart(Command command) {
			System.out.println("onCommandStart : " + command.getmCommandString() );	
		}

		@Override
		public void onCommandExecuted(CommandResponse commandResponse) {
			System.out.println("onCommandExecuted : " + commandResponse.getmResponse());
		}

		@Override
		public void onCommandError(CommandResponse commandResponse) {
			System.out.println("onCommandError : " + commandResponse.getmResponse());

		}
	});
}

public void restoreBackup(File choosedFile, List<AndroidDevice> selectedDeviceArray) {
	System.out.println("restoreBackup");
	Command command = new Command();
	StringBuilder commandStringBuilder = new StringBuilder();

	for (AndroidDevice androidDevice : selectedDeviceArray) {
		commandStringBuilder.append("adb -s " + androidDevice.getmId() + " restore " + CommandUtil.QUOTE + choosedFile.getAbsolutePath() + CommandUtil.QUOTE + " & ");
	}
	//remove last &
	commandStringBuilder.delete(commandStringBuilder.length() -2, commandStringBuilder.length());
	command.setmCommandString(commandStringBuilder.toString());
	mCommandManager.addCommandToQueue(command);
	mCommandManager.executeQueue(new CommandManagerCallBack() {

		@Override
		public void onCommandStart(Command command) {
			System.out.println("onCommandStart : " + command.getmCommandString() );	
		}

		@Override
		public void onCommandExecuted(CommandResponse commandResponse) {
			System.out.println("onCommandExecuted : " + commandResponse.getmResponse());		
		}

		@Override
		public void onCommandError(CommandResponse commandResponse) {
			System.out.println("onCommandError : " + commandResponse.getmResponse());
		}
	});
}

public void installApps(File choosedFile, List<AndroidDevice> selectedDeviceArray) {
	System.out.println("installApps");
	File[] apkFileArray;

	if (choosedFile.isFile()) {
		apkFileArray = new File[] {choosedFile};
	} else {
		//the user choose a folder 
		apkFileArray = choosedFile.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				File file = new File(dir, name);
				boolean acceptFile;

				if (file.isDirectory()) {
					acceptFile = false;
				} else if (name.endsWith(".apk")) {
					acceptFile = true;
				} else {
					acceptFile = false;
				}
				return acceptFile;
			}
		});
	}

	for (int i = 0; i < apkFileArray.length; i++) {
		File apkToInstall = apkFileArray[i];
		Command installApkCommand = new Command();
		StringBuilder installApkCommandStringBuilder = new StringBuilder();

		for (AndroidDevice androidDevice : selectedDeviceArray) {
			installApkCommandStringBuilder.append("adb -s " + androidDevice.getmId() + " install -r " + CommandUtil.QUOTE + apkToInstall.getAbsolutePath() + CommandUtil.QUOTE + " & ");
		}
		//remove the last &
		installApkCommandStringBuilder.delete(installApkCommandStringBuilder.length() - 2, installApkCommandStringBuilder.length());
		installApkCommand.setmCommandString(installApkCommandStringBuilder.toString());
		mCommandManager.addCommandToQueue(installApkCommand);
		mCommandManager.executeQueue(new CommandManagerCallBack() {

			@Override
			public void onCommandStart(Command command) {
				System.out.println("onCommandStart : " + command.getmCommandString() );	
			}

			@Override
			public void onCommandExecuted(CommandResponse commandResponse) {
				System.out.println("onCommandExecuted : " + commandResponse.getmResponse());	
				System.out.println("response = " + commandResponse.getmResponse());
			}

			@Override
			public void onCommandError(CommandResponse commandResponse) {
				System.out.println("onCommandError : " + commandResponse.getmResponse());
				System.out.println("response = " + commandResponse.getmResponse());
			}
		});
	}
}

/**
 * close this jframe
 * open devices details jframe
 * @param choosedFile
 * @param selectedDeviceArray selected device list (without detailed infos)
 */
public void showDeviceListDetails(List<AndroidDevice> selectedDeviceArray, String jframeTitle) {
	System.out.println("showDeviceListDetails");
	//show progress dialog 
	Thread t = new Thread(new Runnable() {
		public void run() {
			mMainView.showProgressDialog("Fetching devices infos....");
		}
	});
	t.start();
	//get the selected devices details
	List<String> selectedDeviceIdList = new ArrayList<String>();

	for (AndroidDevice selectedDevice : selectedDeviceArray) {
		selectedDeviceIdList.add(selectedDevice.getmId());
	}

	List<AndroidDevice> DetailedSelectedDeviceArray = getDeviceDetail(selectedDeviceIdList);
	//close the jframe
	mMainView.setVisible(false); 
	mMainView.dispose(); 
	//open detaisl jframe
	DeviceDetailView deviceDetailView = new DeviceDetailView(DetailedSelectedDeviceArray, jframeTitle);
}

public void sendFile(File choosedFile, List<AndroidDevice> selectedDeviceArray) {
	System.out.println("sendFile");
	Command sendFileCommand = new Command();
	StringBuilder sendFileCommandStringBuilder = new StringBuilder(); 

	for (AndroidDevice androidDevice :  selectedDeviceArray) {
		sendFileCommandStringBuilder.append(
				"adb -s " + androidDevice.getmId() + " push " + CommandUtil.QUOTE + choosedFile.getAbsolutePath() + CommandUtil.QUOTE + " " + CommandUtil.QUOTE + "/sdcard/" + choosedFile.getName() + CommandUtil.QUOTE + " & "
				);
	}
	//remove last &
	sendFileCommandStringBuilder.delete(
			sendFileCommandStringBuilder.length() - 2,
			sendFileCommandStringBuilder.length()
			);

	sendFileCommand.setmCommandString(sendFileCommandStringBuilder.toString());
	mCommandManager.addCommandToQueue(sendFileCommand);
	mCommandManager.executeQueue(new CommandManagerCallBack() {

		@Override
		public void onCommandStart(Command command) {
			System.out.println("onCommandStart : " + command.getmCommandString() );	
		}

		@Override
		public void onCommandExecuted(CommandResponse commandResponse) {
			System.out.println("onCommandExecuted : " + commandResponse.getmResponse());		
		}

		@Override
		public void onCommandError(CommandResponse commandResponse) {
			System.out.println("onCommandError : " + commandResponse.getmResponse());
		}
	});
}



/**
 * 
 * @return connected android devices with basics informations 
 * 
 */
public List<AndroidDevice> getConnectedDeviceList() {
	System.out.println("getConnectedDeviceList");
	List<AndroidDevice> androidDeviceList = new ArrayList<AndroidDevice>();
	Command adbDeviceCommand = new Command();
	adbDeviceCommand.setmCommandString("adb devices");
	mCommandManager.addCommandToQueue(adbDeviceCommand);

	mCommandManager.executeQueue(new CommandManagerCallBack() {
		@Override
		public void onCommandStart(Command command) {
			System.out.println("onCommand start : " + command.getmCommandString());

		}

		@Override
		public void onCommandExecuted(CommandResponse commandResponse) {
			System.out.println("onCommand executed " + commandResponse.getmResponse());
			List<String> deviceIdArrayList = CommandResponseParser.getConnectedDeviceIdArrayList(commandResponse.getmResponse());
			mAndroidDevices = new ArrayList<AndroidDevice>();

			System.out.println("id's " + deviceIdArrayList.toString());
			//get devices info
			for (String deviceId : deviceIdArrayList) {
				Command getPropCommand = new Command();
				getPropCommand.setmCommandString("adb -s " +  deviceId + " shell getprop");
				mCommandManager.addCommandToQueue(getPropCommand);
				mCommandManager.executeQueue(new CommandManagerCallBack() {

					@Override
					public void onCommandStart(Command command) {
						System.out.println("onCommand start : " + command.getmCommandString());
					}

					@Override
					public void onCommandExecuted(CommandResponse commandResponse) {
						System.out.println("onCommandExecuted \n" + commandResponse.getmResponse() );
						//get the device objet from adb shell getprop response 
						AndroidDevice androidDevice = CommandResponseParser.parseGetPropResponse(commandResponse.getmResponse());
						//set the device id
						androidDevice.setmId(deviceId);
						//get device apps
						Command getDeviceAppsCommand = new Command();
						getDeviceAppsCommand.setmCommandString("adb -s " + deviceId + " shell pm list packages -f");
						mCommandManager.addCommandToQueue(getDeviceAppsCommand);
						mCommandManager.executeQueue(new CommandManagerCallBack() {
							@Override
							public void onCommandStart(Command command) {
								System.out.println("onCommandStart : " + command.getmCommandString());
							}

							@Override
							public void onCommandExecuted(CommandResponse commandResponse) {
								System.out.println("onCommandExecuted : " + commandResponse.getmResponse());
								List<String> installAppPackageList = CommandResponseParser.parsePmListPackagesResponse(commandResponse.getmResponse());

								for (String appPackage : installAppPackageList) {
									AndroidApp androidApp = new AndroidApp();
									androidApp.setmPackageName(appPackage);
									androidDevice.addApp(androidApp);
								}
							}

							@Override
							public void onCommandError(CommandResponse commandResponse) {
								System.out.println("onCommandError : " + commandResponse.getmResponse());

							}
						});
						//add the device to connected devices list
						androidDeviceList.add(androidDevice);
					}

					@Override
					public void onCommandError(CommandResponse commandResponse) {
						System.out.println("onCommandError : " + commandResponse.getmResponse());
					}
				});
			}
		}

		@Override
		public void onCommandError(CommandResponse commandResponse) {
			System.out.println("onCommandError : " + commandResponse.getmResponse());
		}
	});
	return androidDeviceList;
}


/**
 * 
 * @param deviceIdArray 
 * @return return list of android device with details infos
 */
public List<AndroidDevice> getDeviceDetail(List<String> deviceIdArray) {
	System.out.println("getDeviceDetails " + deviceIdArray.toString());
	List<AndroidDevice> androidDeviceList = new ArrayList<AndroidDevice>();

	for (String deviceId : deviceIdArray) {
		Command command = new Command();
		command.setmCommandString("adb -s " +  deviceId + " shell getprop");
		mCommandManager.addCommandToQueue(command);
		mCommandManager.executeQueue(new CommandManagerCallBack() {

			@Override
			public void onCommandStart(Command command) {
				System.out.println("onCommand start : " + command.getmCommandString());

			}

			@Override
			public void onCommandExecuted(CommandResponse commandResponse) {
				System.out.println("onCommandExecuted \n" + commandResponse.getmResponse() );
				//get the device objet from adb shell getprop response 
				AndroidDevice androidDevice = CommandResponseParser.parseGetPropResponse(commandResponse.getmResponse());
				//set the device id
				androidDevice.setmId(deviceId);
				//get device apps
				Command getDeviceAppsCommand = new Command();
				getDeviceAppsCommand.setmCommandString("adb -s " + deviceId + " shell pm list packages -f");
				mCommandManager.addCommandToQueue(getDeviceAppsCommand);
				mCommandManager.executeQueue(new CommandManagerCallBack() {

					@Override
					public void onCommandStart(Command command) {
						System.out.println("onCommandStart : " + command.getmCommandString());

					}

					@Override
					public void onCommandExecuted(CommandResponse commandResponse) {
						System.out.println("onCommandExecuted : " + commandResponse.getmResponse());
						List<String> installAppPackageList = CommandResponseParser.parsePmListPackagesResponse(commandResponse.getmResponse());


						//for any package get the androidApp instance
						for (String packageName : installAppPackageList) {
							Command getAppInfoCommand = new Command();
							getAppInfoCommand.setmCommandString("adb -s " + deviceId + " shell dumpsys package " + packageName);
							mCommandManager.addCommandToQueue(getAppInfoCommand);
							mCommandManager.executeQueue(new CommandManagerCallBack() {

								@Override
								public void onCommandStart(Command command) {
									System.out.println("onCommandStart : " + command.getmCommandString());

								}

								@Override
								public void onCommandExecuted(CommandResponse commandResponse) {
									System.out.println("onCommandExecuted : " + commandResponse.getmResponse());
									AndroidApp androidApp = CommandResponseParser.parseDumpsysPackageResponse(commandResponse.getmResponse());
									androidDevice.addApp(androidApp);
								}

								@Override
								public void onCommandError(CommandResponse commandResponse) {
									System.out.println("onCommandError : " + command.getmCommandString());

								}
							});
						}
					}

					@Override
					public void onCommandError(CommandResponse commandResponse) {
						System.out.println("onCommandError : " + command.getmCommandString());
					}
				});
				//add the device to connected devices list
				androidDeviceList.add(androidDevice);
			}

			@Override
			public void onCommandError(CommandResponse commandResponse) {
				System.out.println("onCommandError : " + command.getmCommandString());

			}
		});

	}
	return androidDeviceList;
}
}
