# EZmessaging

A library used to send, receive, and query existing SMS and MMS messages.

## Setup
- The minSdk is `26`
- Add the Jitpack repository to your build file
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```
- Add the dependency to your app `build.gradle`
```kotlin
implementation("com.github.pscott451:EZmessaging:1.4.3")
```
- Add the required permissions to your `AndroidManifest`
```xml
    <uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_MMS" />
<uses-permission android:name="android.permission.RECEIVE_WAP_PUSH" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```
- Add the following intent filter to one of your activities so your app can be set as the default messaging app
```xml
<activity
    android:name=".ui.activity.SetupActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <intent-filter>
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <!-- The following is required for the app to be set as the default messaging app -->
        <action android:name="android.intent.action.SEND" />
        <action android:name="android.intent.action.SENDTO" />
        <action android:name="android.intent.action.SEND_MULTIPLE" />
        <data android:scheme="sms" />
        <data android:scheme="smsto" />
        <data android:scheme="mms" />
        <data android:scheme="mmsto" />
    </intent-filter>
</activity>
```

## Receiving Messages
- To be able to receive messages, create two broadcast receivers and extend the `MessageReceivedBroadcastReceiver`. One will need to be created for both SMS and MMS messages.

### Example receivers:
```kotlin
class SmsReceiver: MessageReceivedBroadcastReceiver() {

    override fun onMessageReceived(message: Message) {
        // Do smart things with the message
    }
}
```
```kotlin
class MmsReceiver: MessageReceivedBroadcastReceiver() {

    override fun onMessageReceived(message: Message) {
        // Do smart things with the message
    }
}
```
### Add the created receivers to the manifest
```xml
<receiver
    android:name=".receiver.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter>
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
        <action android:name="android.provider.Telephony.SMS_DELIVER" />
    </intent-filter>
</receiver>

<receiver
    android:name=".receiver.MmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_WAP_PUSH">
    <intent-filter>
        <action android:name="android.provider.Telephony.WAP_PUSH_DELIVER" />
        <data android:mimeType="application/vnd.wap.mms-message" />
    </intent-filter>
</receiver>
```

## Usage
- The `ContentManager` makes it very EZ (😁) to send messages.
### Getting an Instance of the `ContentManager`
- The `ContentManager` is provided as a singleton via `Hilt`, so depending on where it's needed, it may have to be injected differently (e.g. tagging an Activity with `@AndroidEntryPoint` or a `ViewModel` with `@HiltViewModel`). See the [Hilt Documentation](https://developer.android.com/training/dependency-injection/hilt-android) for more info.
```
@AndroidEntryPoint
class MainActivity: ComponentActivity() {

    @Inject
    lateinit var contentManager: ContentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
```

- Before the `ContentManager` can be used, it must be initialized.
```kotlin
private fun initContentManager() {
    // `initializedState` is a StateFlow that emits Initialized when it's ready to use
    contentManager.initializedState.onEach {
        if (it is Initializable.Initialized) {
            // Ready
        }
    }.launchIn(lifecycleScope)
    contentManager.initialize()
}
```

### Sending Messages
#### SMS
```kotlin
contentManager.sendSmsMessage(
    address = recipient, // The phone number of the recipient. (e.g. "5553231122")
    text = message, // The message to send. (e.g. "Good morrow, good sir!")
    onSent = { sendResult ->  // Invoked with [MessageSendResult.Success] if the message was successfully sent.
        if (sendResult is MessageSendResult.Success) {
            // :)
        } else {
            // :(
        }
    },
    onDelivered = { wasDelivered ->  // A boolean indicating when the message was delivered to the recipient.  Depending on the carrier and device, this may not always be available.
                
    }
)
```

#### MMS
- Sending an MMS message requires creating `MessageData`. It can either be `MessageData.Text`, `MessageData.Image`, or `MessageData.ContentUri`.
- The `MessageData.ContentUri` is Convenient when using a photo picker such as [this one](https://developer.android.com/training/data-storage/shared/photopicker) since it returns a uri.
- See `ContentManager.SupportedMessageTypes` for supported mime types.
```kotlin
val textMessageData = MessageData.Text(text = "I'm a message!")
val imageMessageData = MessageData.Image(bitmap = /* a bitmap */, mimeType = "image/jpeg")
val contentUriMessageData = MessageData.ContentUri(uri = "content://com.android.providers.downloads.documents/document/20")
```

```kotlin
contentManager.sendMmsMessage(
    message = messageData,  // A `MessageData` object. 
    recipients = recipients,  // An Array of recipients. (e.g. arrayOf("5553231122", "5553335555"))
    onSent = { sendResult ->  // Invoked with [MessageSendResult.Success] if the message was successfully sent.
        if (sendResult is MessageSendResult.Success) {
            // :)
        } else {
            // :(
        }
    }
)
```

### Querying Existing Messages
- Simply call `getAllMessages()` to return a `List<Message>` of all existing messages.
- NOTE: This may potentially take awhile to return depending on the number of messages the user currently has.
```kotlin
contentManager.getAllMessages()
```
- `getMessagesByParams()` will return parameter based messages.
- NOTE: `afterDateMillis` may not always return as expected. Some apps (*cough* Google *cough* 🙄) time stamp received messages in seconds instead of milliseconds.
```kotlin
contentManager.getMessagesByParams(
    exactText = "some text", // returns all messages that match "some text".
    containsText = "some text", // returns all messages that contain the substring "some text".
    afterDateMillis = 1717200000000 // returns all messages received after Jun 1, 2024.
)
```
