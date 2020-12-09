/* global config, browser, client */

//Haven't found a good way to cenralize this string, so it has to be set both here
//and in manifest.json. It does have to be consistent in order to work.
var extension_id = "oath-wallet@Levitat0r";
var extension_name = "oath-wallet";
var pin_service_name = "oath-wallet-service";
var extension_title = "OATH Wallet";

//Keep in mind that there are still modern embedded system which are really, really slow
//Something like a Pi Zero probably can't even run Firefox, though
var release_config = {
  debug: false,
  startup_timeout: 60 * 1000,  
  message_timeout: 30 * 1000
  
};

//var debug_config_timeout = 60 * 1000 * 60;
var debug_config_timeout = 1000 * 30;
var debug_config = {
    debug: true,
    startup_timeout: release_config.startup_timeout,
    message_timeout: debug_config_timeout
};

config = debug_config;

function debug(msg){
    if(config.debug)
        console.log("DEBUG: " + msg);
}

function notify2(title, message){
    console.log(title + ": " + message);
    browser.notifications.create({
            "type": "basic",
            "title": title,
            "message": message
    });
}

function notify(message){
    notify2(extension_title, message);
}

//Notify of exception
function notify_ex(message, ex){
    notify2(extension_title + " ERROR", message + (ex != null ? ": " + ex.message : "") );
}

function fetch_pin_command_failure(message){
	notify("Unexpected error retrieving active browser window: " + message);
}

function fetch_pin_for_window(window){
	var tab = browser.tabs.getCurrent();
	console.log(JSON.stringify(window));
	notify2("DEBUG", "The window location is: " + tab.url);
}

function fetch_pin_for_tab(tab){
	console.log("tab url is: " + tab.url);
	console.log(JSON.stringify(tab));
}

function logmsg(msg){
    console.log(msg);
}

function logmsg2(msg, ex){
    logmsg(msg + ": " + ex.message + "\n" + (ex.stack != null ? ex.stack : ""));
}

function do_throw(err){
	throw err;
}

function show_error(err){
	var msg = "Unexpected error fetching PIN: " + err;
	notify2(extension_title + err, msg);
	console.logmsg(msg);
}

var pin_probe_function_name="xxxxxxx_pin_query_probe_xxxxxxx";

//We need to define a function in the target web page's environment in order to create a private scope
//to work in, but first, we need to make sure the function name isn't clashing. This is unlikely unless
//someone were to contrive it that way.
var namespace_probe_script=`
	debugger;
	browser.runtime.sendMessage( "${extension_id}", { type: "namespace-check", value: "${pin_probe_function_name}" in this }, {} );
	`

//We choose a really weird function name to try to avoid clashing with
var pin_probe_script = `
	browser.runtime.sendMessage( extension_id, { type: "pin-request", url: location }, {} );
	console.logmsg("Sent OATH Wallet PIN request to extension");
`

var pin_request_state = null;

async function send_pin_request_probe(){
	var tabs = await browser.tabs.query( { active: true, currentWindow: true, discarded: false, hidden:false} );
	if(tabs.length > 1)
		throw "A query for the active web page returned more than one result. This is confusing, so I can't fetch a PIN.";

	if(tabs.length < 1)
		throw "Unexpectedly unable to find the current active web page. This is probably a bug.";

	tab = tabs[0];
	pin_request_state = new PinRequest(tab, tab.url);
	browser.tabs.executeScript( { code: namespace_probe_script } ).then( ()=>{}, (err)=>{ pin_request_state=null; show_error(err); } );
}

//The tab object visible to the background script is a different instance than that
//which the user page sees. It's probably excessive and brittle to write a deep tab-equality function.
//Also, the Mozilla docs say that the tab IDs are unique in a given browser session. Assuming they are
//never reused, then that should be sufficient.
function validate_browser_sender( sender ){
	//var tabs_equal = deepEqual(pin_request_state.tab, sender.tab);
	var a = pin_request_state.tab;
	var b = sender.tab;
	return a != null && b !=null && a.url == b.url && a.id != browser.tabs.TAB_ID_NONE && a.id != null && a.id == b.id;
}

function handleMessage(message, sender, response_f){

    //debugger;

    //Validate sender. Log jiggerypokery.
    var result = validate_browser_sender(sender);
    if(!result){
        console.logmsg( "Dropped spurious (suspicious) message from: '" + sender.url);
        return;
    }

    if(message.type == "namespace-check"){
        if( message.value ){
            notify(`${oath-wallet} ERROR`, "This web page contains script definitions that conflict with our functionality, so we cannot proceed. Also, this is strange and probably should not happen.");
            pin_request_state = null;
            return;
        }
        else{
            notify("EXTRA WOOTS", "WOOTS are plentiful");
        }
    }
}

/*
async function fetch_pin_command(){

	//PIN request function is not reentrant. One request at a time, plz.
	if(pin_request_state != null){
		notify(`${extension_id} Warning', "Ignored a PIN request because another request is pending`);
		return;
	}

	tab = await get_active_tab();
	console.log("Processing PIN request for site: " + tab.url);
}
 */

//Back-end related items
var backend = null;

//
//Message definitions
//

class Message{
    type;
    
    //static variables overload each other in what would be termed a "virtual" manner in C++
    //i.e. subclass overloads are visible in the base, via the prototype
    constructor(proto){
        this.type = proto.static_type;
    }
}

class ErrorMessage extends Message{
    
    static static_type = "error";
    
    message;
    
    constructor(){
        super(ErrorMessage);
    }
    
    process = (state) => {
        notify_ex(this.message);
    }
};

class SessionMessage extends Message{
    session_id;
    
    constructor(proto){
        super(proto);
    }
    
};

class HelloMessage extends SessionMessage{
    static static_type="hello";
    
    constructor(){
        super(HelloMessage);
    }    
};

class PinRequest extends SessionMessage{        
    static static_type = "pin_request";
    url;
    
    constructor(session, url_in){
        super(PinRequest);
        this.session_id = session;
        this.url = url_in;
    }    
};

class PinReply extends SessionMessage{
    static static_type="pin_reply"
    url;
    cred;
    pin;
    
    constructor(){
        super(PinReply);
    }
    
    process = (state) => {
        notify( "WOOTS: " + JSON.stringify(this) );
    }    
};

class Notification extends SessionMessage{
    static static_type="notification";
    message;    
    
    constructor(){
        super(Notification);
    }
    
    process = (state) => {
        notify(this.message);
    }    
};

class ByeMessage extends Message{
    static static_type="bye";
            
    constructor(){
        super(ByeMessage);
    }   
    
    process = (state) => {
        state.client.disconnect();
        debug("Disconnect on client-side");
    };
    
};

class MessageRegistry{
    messages = {};
    
    register = (classes) => {
        classes.forEach( (cls) => {           
            this.messages[cls.static_type] = cls;
        });
    }
    
    get = (name) => this.messages[name];        
}

//Enumerate the types of messages which the client will recognize and construct from protocol messages
var message_classes = [ PinReply, Notification, ErrorMessage, ByeMessage ];
var message_registry = new MessageRegistry();
message_registry.register(message_classes);

class DisconnectedException extends Error{
};

function handle_event(f){
    try{
        f();
    }
    catch(ex){
        logmsg2("Warning: uncaught exception in event handler", ex);
    }    
}

//Native messages go into the queue like this
class NativeMessage extends Event{
    message;
    
    constructor(msg){
        super("native_message");
        this.message = msg;
    }
};

//Native messages are pulled out of the queue and are dispatched like this
class ClientMessageEvent extends Event{
    message;
    
    constructor(msg){
        super(msg.type);
        this.message = msg;
    }
}

class DisconnectEvent extends Event{
    constructor(){
        super("disconnect");
    }
}

const sleep = (milliseconds) => {
  return new Promise(resolve => setTimeout(resolve, milliseconds));
};

class MutexQueueFull extends Error{    
}

//Ok, so it's kind of mindbending that there should be concurrency issues in stupid javascript, but I guess there sort of are.
//If nothing else, out-of-order crap may happen in the debugging environment. Sometimes you post a native message, and then events
//start mysteriously firing before you return from the script or call the await operator.
//We will go out on a limb and assume that arithmetic operations are atomic, so you cannot do ++x in two places and still get x+1
//Let's additionally hope there are no races between push() and shift()
class Mutex{
    x=0;
    max_queue=Number.MAX_SAFE_INTEGER;
    wait_queue = [];
    
    constructor(queue_size){
        if(queue_size != null)
            this.max_queue = queue_size;
    }
    
    //true: block
    //false: return immediately
    lock(block){
        var result = (++this.x === 1);
        if(result)
            return new Promise( (resolve, reject)=>{ resolve(true); } ); //sucess, return a resolved promise
        else{
            if(block && x - 1 <= this.max_queue){
                //temporary failure. Return a pending promise and stick the resolve function in the wait queue.
                var resolve2;
                var prom = new Promise( (resolve, reject)=>{ resolve2 = resolve;  } );
                this.wait_queue.push(resolve2);
                return prom;
            }
            else{
                --x;
                return new Promise( (resolve, reject) => { reject(new MutexQueueFull()); } );
            }
        }
    }
    
    unlock(){                                
        var resolve;
        var newx=--this.x;
                
        //There could be a race where x is incremented but the queue has not been updated.
        //So, unless we are the last Mutex owner, spin until one queue element is released
        if(newx > 0){
            while( (resolve = this.wait_queue.shift()) == null ){  }
            resolve(true);
        }                     
    }
}


//The connection to the backend process
//It will internally connect and disconnect as needed
class BackendLink{
    port=null;
    connected=false;
    
    client_state = {}; //State data message objects might need to keep track of what's going on
    session_message_handlers = new EventTarget();
    persistent_message_handlers = new EventTarget();
    session_pending_timeouts = {};
    
    //Ok, so javascript is supposed to have a central event loop that dispatches events in sequence from a queue
    //when the script is idle so that the events don't race with the script contents, so I am surprised
    //to discover that native-messaging events happen pretty much completely asynchronously (hopefully not reentrantly).
    //So, we implement our own queue
    //to stuff them into in order to keep them in order until we are able to get around to them.
    //Also, I found that debugging of native message events is otherwise flaky and unreliable under Firefox
    //So, by queing them up first and then later dispatching them as ordinary DOM events, that avoids those problems
    //and makes debugging much easier. The native-messaging API is a half-baked mess. I suppose the debugging problems
    //could crop up again in the case where the native-message event encounters an idle queue and is forced to kickstart it
    //because then the events are being handled on the native message call chain again.
    event_queue = [];
    m_consume = false;
    
    //Ensure that drain() is not reentrant, and there can be only one drain_events() operation in the wait queue,
    //which is all that is needed to sustain the draining sequence until it is done
    drain_mutex = new Mutex(1);
    
    constructor(){
        this.persistent_message_handlers.addEventListener("disconnect", this.internal_disconnect_handler);        
    }
    
    connect = () => {
        this.port = browser.runtime.connectNative('com.levitator.oath_wallet_service');
        this.connected = this.port.error === null || this.port.error.length === 0;
        if(this.connected){
            this.port.onMessage.addListener( this.message_handler );    
            this.port.onDisconnect.addListener( this.disconnect_handler );
        }
        else
            throw new Error(this.port.error);
    }
    
    disconnect = () => {
        this.port.disconnect();
        this.connected = false;
        
        //Client-side disconnect does not raise a disconnect event even though a disconnect takes place
        //so we synthesize it
        this.disconnect_handler();
        
        debug("Client-side port disconnect");
    }
    
    get consume(){
        return this.m_consume;
    }
    
    set consume(v){
        this.m_consume = v;
        this.drain_events();
    }
    
    //Can be interrupted while processing events if an event handler sets "consume" false
    //Returns the number of remaining events in the queue, which will often be 0
    drain_events = ()=> {
        
        //If we are the first to fail to lock, we go into the wait queue,
        //otherwise, our request is dropped and gets completed by whoever is already waiting
        return this.drain_mutex.lock(true).then( () => {
            var evt;
            while(this.m_consume){
                evt = this.event_queue.shift();
                if(evt == null)
                    break;
                
                if(evt.type == "native_message"){
                    var msg = evt.message;
                    var cls = message_registry.get(msg.type);
                    if(cls == null)
                        throw new Error( "Unknown message type from back-end: " + msg.type );

                    //msg is constructed from json
                    //msg2 is constructed from the class definition in this script
                    //we take the "enumerable properties" from msg and assign them to msg2
                    //so that msg2 is the union of json data and the in-script method definitions
                    //from the message classes, most notably the process() method;
                    var msg2 = new cls();
                    Object.assign(msg2, msg);
                    evt = new ClientMessageEvent(msg2);
                    this.general_message_consumer(evt); //call process() on the message
                }
                
                //After the message is finished with its inherent processing, dispatch to any listeners
                this.session_message_handlers.dispatchEvent(evt);
                this.persistent_message_handlers.dispatchEvent(evt);
            }
            return this.event_queue.length;
        }).catch( (ex) => {
            if(!(ex instanceof MutexQueueFull))
                throw ex;
        }).finally(this.drain_mutex.unlock());
    }
    
    message_handler = (msg) => {
        try{
            debug("MESSAGE IN: " + JSON.stringify(msg));
            this.event_queue.push(new NativeMessage(msg));
            this.drain_events();
        }
        catch(ex){
            notify_ex("Failed processing message from back-end. There is probably some problem with the installation. " + 
                    "The front and backends may have mismatched versions.", ex);
        }
    }
    
    //Call process() on the message and let it decide what to do
    general_message_consumer = (evt) => {
        try{
            evt.message.process(this.client_state);
        }
        catch(ex){
            notify_ex("Error processing back-end message", ex);
        }
    }
            
    disconnect_handler = () => {
        this.event_queue.push(new DisconnectEvent());        
        this.drain_events();
    }
        
    internal_disconnect_handler = (evt) => {
        
        //Stop consuming messages until the object user is ready for the
        //next session
        this.consume = false;
        
        this.connected = false;
        
        //Trigger session-scope timeouts early upon session close
        Object.entries(this.session_pending_timeouts).forEach( (obj)=>{            
            clearTimeout(obj[0]);
            obj[1]();
            debug("Triggered a message timeout early due to disconnect");
        });
        this.session_pending_timeouts = [];
        this.session_message_handlers = new EventTarget();        
    }
    
    send_message = (msg, timeout) => {
        if(!this.connected)
            this.connect();
                        
        if(Array.isArray(msg))
            this.port.postMessage(msg);
        else
            this.port.postMessage( [ msg ] );
        
        this.consume = true;
        var prom2 = this.until_disconnect(timeout);
        var prom1 = this.until_session_message( ByeMessage, timeout ).then( prom2 );
        this.drain_events();
        return prom1;
    }
    
    until_disconnect = (timeout)=>{
        var resolver=null;
        var timeout_id;
        var prom = new Promise((resolve, reject)=>{ 
            resolver = resolve; 
            timeout_id = setTimeout(()=>{
                reject(new Error("timeout"));
                debug("disconnect promise timed out");
            }, timeout); 
        });
    
        this.session_message_handlers.addEventListener( "disconnect", () => {
            clearTimeout(timeout_id);
            resolver(null); 
            debug("disconnect promise resolved");
        });
        return prom;
    }        
    
    static add_message_handler = (cls, handler, target) =>{        
        target.addEventListener(cls.static_type, handler);                
        debug("registered listener for message type: " + cls.static_type);
    }
    
    when_session_message = (cls, handler) => {        
        BackendLink.add_message_handler(cls, handler, this.session_message_handlers);
    }
    
    when_message_ever = (cls, handler) => {
        this.add_message_handler(cls, handler, this.persistent_message_handlers);
    }
    
    when_message = (cls, handler, ever) => {
        ever ? this.when_message_ever(cls, handler) : this.when_session_message(cls, handler);
    }
    
    remove_message_handler_from_collection = (cls, handler, target) => {        
        target.removeEventListener(cls.static_type, handler);
    }
    
    remove_message_handler = (cls, handler) => {
       this.remove_message_handler_from_collection(cls, handler, this.session_message_handlers);
       this.remove_message_handler_from_collection(cls, handler, this.persistent_message_handlers);
       var timeout = this.session_pending_timeouts[handler];
              
       if(timeout != null){
           debug("found a timeout object to cancel");
           clearTimeout(timeout.timeout_id);
           delete this.session_pending_timeouts[handler];
       }          
    }
    
    until_message = (cls, timeout, ever) => {
        var timeouts = this.session_pending_timeouts;
        var new_handler;
        var remove_func = this.remove_message_handler;
        var timeout_id_box = [];
        var prom = new Promise( (resolve, reject)=> {            
            new_handler = (evt) => {
                clearTimeout(timeout_id_box[0]);
                delete this.session_pending_timeouts[timeout_id_box[0]];
                remove_func(cls, this);
                resolve(evt);
                debug("successfully resolved message promise: " + cls.static_type);
            };
            //remove_func(cls, new_handler); //Probably not necessary?
            var new_handler2=new_handler;
                        
            //If session-scope
            if(timeout != null){
                var timeout_func = (err)=>{ remove_func(cls, new_handler2); reject(err); debug("got timeout for message type: " + cls.static_type); };
                timeout_id_box[0] = setTimeout( timeout_func, timeout);
                if(!ever)
                    this.session_pending_timeouts[timeout_id_box[0]] = timeout_func;
            }
        });
        //debugger;        
        this.when_message(cls, new_handler, ever);
        return prom;
    }
    
    until_message_ever = (cls, timeout) => { return this.until_message(cls, timeout, true); }
    until_session_message = (cls, timeout) => { return this.until_message(cls, timeout, false); }
};

class Client{
    link;
    
    constructor(){
        this.link = new BackendLink();                  //Initialize connection state
        this.link.client_state['client'] = this;
    }
    
    start(){
        return this.link.send_message(new HelloMessage(), config.startup_timeout);     //Send a no-op message to launch the backend/gui
    }
    
    disconnect(){
        this.link.disconnect();
    }
    
    //Identify the browser tab/session which received the PIN-request keystroke
    get_active_tab = async () => {
        var tabs = await browser.tabs.query( { active: true, currentWindow: true, discarded: false, hidden:false} );
        if(tabs.length > 1)
                throw new Error("A query for the active browser tab returned more than one result. This is confusing, so I can't fetch a PIN.");

        if(tabs.length < 1)
                throw new Error("Unexpectedly unable to find the current active browser tab. This is probably a bug.");
        
        //If this should pose a problem, then we could go by URL, too, but that has the potential to be ambiguous
        //But we can throw an error for that
        if(tabs[0].id == null)
            throw new Error("This browser does not support the 'id' property on tab objects.");
               
        if(tabs[0] == tabs.TAB_ID_NONE)
            throw new Error("This tab does not contain a Web page and is not eligible for OATH retrieval");
        
        return tabs[0];
    }
    
    pin_query = async ()=> {
        this.link.until_session_message(PinReply, 3000).catch( () =>
            notify("OATH PIN requested.\nYou may need to activate or touch your key device to proceed."));
        var session_id = (await this.get_active_tab()).id;
                
        return this.link.send_message( new PinRequest(session_id, "https://www.facebook.com/blah/blah/blah") );        
    }
}

//backend.send_message( new PinRequest(123, "https://www.facebook.com/blah/blah/blah") ).then( ()=>{
//    backend.send_message( new PinRequest(321, "https://www.facebook.com/blah/blah/blah") );
//});

function browser_command_handler (command) {
  try{
	if (command === "oath-sign-in")
            fetch_pin_command();
	else
	  throw new Error("Command: '" + command + "' is not recongized. Perhaps someone does us a bamboozle."); //Should not happen

  }catch(err){
	  show_error(err);
  }
}

// Main
client = null;

try{
    notify("starting");    
    client = new Client();
    
    client.start().then( ()=>{
        browser.commands.onCommand.addListener(browser_command_handler); //Listen for the sign-in hotkey
        notify("READY");
    }).catch( (ex)=>{init_fail(ex); });
}
catch(ex){
    init_fail(ex);
}

function init_fail(ex){
    var msg = "Failed connecting to back-end service. The extension may not be installed properly";
    notify_ex(msg, ex);
}

async function fetch_pin_command(){
    try{
        await client.pin_query();
    }
    catch(ex){
        notify_ex("Error retrieving PIN number", ex);
    }
}

