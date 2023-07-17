import UIKit
import Flutter
import VonageClientSDKVoice

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
    enum SdkState: String {
        case loggedOut = "LOGGED_OUT"
        case loggedIn = "LOGGED_IN"
        case wait = "WAIT"
        case onCall = "ON_CALL"
        case error = "ERROR"
    }
    
    var vonageChannel: FlutterMethodChannel?
    var client: VGVoiceClient? = nil
    var callID: String?
    
    override func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        initClient()
        addFlutterChannelListener()
        
        GeneratedPluginRegistrant.register(with: self)
        return super.application(application, didFinishLaunchingWithOptions: launchOptions)
    }
    
    func initClient() {
        client = VGVoiceClient()
        let config = VGClientConfig(region: .US)
        client.setConfig(config)
    }
    
    func addFlutterChannelListener() {
        let controller = window?.rootViewController as! FlutterViewController
        
        vonageChannel = FlutterMethodChannel(name: "com.vonage",
                                             binaryMessenger: controller.binaryMessenger)
        vonageChannel?.setMethodCallHandler({ [weak self]
            (call: FlutterMethodCall, result: @escaping FlutterResult) -> Void in
            guard let self = self else { return }
            
            switch(call.method) {
            case "loginUser":
                if let arguments = call.arguments as? [String: String],
                   let token = arguments["token"] {
                    self.loginUser(token: token)
                }
                result("")
            case "makeCall":
                self.makeCall()
                result("")
            case "endCall":
                self.endCall()
                result("")
            default:
                result(FlutterMethodNotImplemented)
            }
        })
    }
    
    func loginUser(token: String) {
        client?.createSession(token, sessionId: nil) { error, sessionId in
            if (error != nil) {
                self.notifyFlutter(state: .error)
            } else {
                self.notifyFlutter(state: .loggedIn)
            }
        }
    }
    
    func makeCall() {
        client.serverCall(["to": "PHONE_NUMBER"]) { error, callId in
                    DispatchQueue.main.async { [weak self] in
                        guard let self else { return }
                        if error == nil {
                            self.callID = callId
                            self.notifyFlutter(state: .onCall)
                        } else {
                            self.notifyFlutter(state: .error)
                        }
                    }
                }
    }
    
    func endCall() {
        client.hangup(callID) { error in
                    DispatchQueue.main.async { [weak self] in
                        guard let self else { return }
                        if (error != nil) {
                            self.notifyFlutter(state: .error)
                        } else {
                            self.callID = nil
                            self.notifyFlutter(state: .loggedIn)
                        }
                    }
                }
    }
    
    func notifyFlutter(state: SdkState) {
        vonageChannel?.invokeMethod("updateState", arguments: state.rawValue)
    }
}
