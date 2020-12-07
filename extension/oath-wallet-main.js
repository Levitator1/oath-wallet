/* global config, browser */

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
  message_timeout: 10 * 1000
  
};

var debug_config_timeout = 60 * 1000 * 60;
//var debug_config_timeout = 1000 * 5;
var debug_config = {
    debug: true,
    startup_timeout: debug_config_timeout,
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
    logmsg(msg + ": " + ex.message + "\n" + ex.stack != null ? ex.stack : "");
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


//Identify the tab which received the PIN-request keystroke
async function get_active_tab(){
	var tabs = await browser.tabs.query( { active: true, currentWindow: true, discarded: false, hidden:false} );
	if(tabs.length > 1)
		throw "A query for the active web page returned more than one result. This is confusing, so I can't fetch a PIN.";

	if(tabs.length < 1)
		throw "Unexpectedly unable to find the current active web page. This is probably a bug.";

	return tabs[0];
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
    
    constructor(){
        super(ErrorMessage);
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
    
};

class Notification extends SessionMessage{
    static static_type="notification";
    message;    
    
    constructor(){
        super(Notification);
    }
    
};

class ByeMessage extends Message{
    static static_type="bye";
    
    constructor(){
        super(ByeMessage);
    }    
};

var message_registry = [ PinReply, Notification, ErrorMessage, ByeMessage ];

//Map a key to a set of values
class MultiMap{
    data = {};
    
    merge = (rhs) => {
        Object.keys(rhs.data).forEach( (k)=>{
            var obj = this.data[k];
            if(obj === undefined)
                this.data[k] = obj = {};
            Object.assign( obj, rhs.data[k] );
        });        
    }
    
    add = (k,v) => {
        var set = data[k];
        if(set === undefined)
            data[k] = { v: null };
        else
            set[v]=null;
    }
    
    get = (k) => {
        return data[k];
    }
    
    require = (k) => {
        var result = data[k];
        if(result === undefined)
            throw new RangeError("MultiMap key out of range");
        else
            return result;
    }
    
    remove_of_key = (k) => {
        data[k] = undefined;
    }
    
    remove = (k, v) => {
        var set = data[k];
        if(set === undefined)
            return;
        
        delete set[v];
        if(Object.keys(set).length <= 0)
            delete data[k];
    }
    
    forEach = (f) => {
        Object.keys(data).forEach( (set) => {
            Object.keys(set).forEach( (k)=>{
                f(k);
            });
        });
    }    
}

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

class NativeMessage extends Event{
    message;
    
    constructor(msg){
        super(msg.type);
        this.message = msg;
    }
};

class DisconnectEvent extends Event{
    constructor(){
        super("disconnect");
    }
}

//Ok, so it's kind of mindbending that there should be concurrency issues in stupid javascript, but I guess there sort of are.
//If nothing else, out-of-order crap may happen in the debugging environment. Sometimes you post a native message, and then events
//start mysteriously firing before you return from the script or call the await operator.
//We will go out on a limb and assume that arithmetic operations are atomic, so you cannot do ++x in two places and still get x+1
//Let's additionally hope there are no races between push() and shift()
class Mutex{
    x=0;
    wait_queue = [];
    
    lock(){
        var result = (++this.x === 1);
        if(result)
            return new Promise( (resolve, reject)=>{ resolve(true); } ); //sucess, return a resolved promise
        else{
            //temporary failure. Return a pending promise and stick the resolve function in the wait queue.
            var resolve2;
            var prom = new Promise( (resolve, reject)=>{ resolve2 = resolve;  } );
            this.wait_queue.push(resolve2);
            return prom;
        }
    }
    
    unlock(){                                
        var resolve;
        var newx=--this.x;
                
        //There could be a race where x is incremented but the queue has not been updated.
        //So, unless we are the last Mutex owner, spin until one queue element is released
        if(newx > 0){
            while( (resolve = this.wait_queue.shift()) == null ){}
            resolve(true);
        }                     
    }
}


//The connection to the backend process
//It will internally connect and disconnect as needed
class BackendLink{
    port=null;
    connected=false;
    
    session_message_handlers = new EventTarget();
    persistent_message_handlers = new EventTarget();
    session_pending_timeouts = {};
    
    //Ok, so javascript is supposed to have a central event loop that dispatches events in sequence from a queue
    //when the script is idle so that the events don't race with the script contents, so I am surprised
    //to discover that native-messaging events happen pretty much completely asynchronously. So, we implement our own queue
    //to stuff them into in order to keep them in order until we are able to get around to them.
    //Also, I found that debugging of native message events is otherwise flaky and unreliable under Firefox
    //So, by queing them up first and then later dispatching them as ordinary DOM events, that avoids those problems
    //and makes debugging much easier. The native-messaging API is a half-baked mess.
    event_queue = [];
    m_consume = false;
    drain_mutex = new Mutex(); //Ensure that drain() is not reentrant, and any overlapping attempts are queued serially
    
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
        return this.drain_mutex.lock().then( () => {
            var evt;
            while(this.m_consume){
                evt = this.event_queue.shift();
                if(evt == null)
                    break;
                this.session_message_handlers.dispatchEvent(evt);
                this.persistent_message_handlers.dispatchEvent(evt);
            }
            return this.event_queue.length;
        }).finally(this.drain_mutex.unlock());
    }
    
    message_handler = (msg) => {
        this.event_queue.push(new NativeMessage(msg));
        if(this.m_consume && this.event_queue.length == 0)
            this.drain_events();
    }
            
    disconnect_handler = () => {
        this.event_queue.push(new DisconnectEvent());
        if(this.consume)
            this.drain_events();
    }
        
    internal_disconnect_handler = (evt) => {
        
        //Stop consuming messages until the object user is ready for the
        //next session
        this.consume = false;
        
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
        
        //Create these first because events come rolling in immediately before return to the browser
        //How that happens is a total mystery, as it's supposed to require a return to the event loop
        //Maybe postMessage() calls the event loop
        var promises = [this.until_session_message( ByeMessage, config.startup_timeout ), this.until_disconnect(config.message_timeout)];
        
        if(Array.isArray(msg))
            this.port.postMessage(msg);
        else
            this.port.postMessage( [ msg ] );
        
        //debugger;
        //return this.until_session_message( ByeMessage, config.startup_timeout ).then(this.until_disconnect(config.message_timeout));
        
        return promises;
    }
    
    until_disconnect = (timeout)=>{
        var resolver=null;
        var prom = new Promise((resolve, reject)=>{ 
            resolver = resolve; 
            setTimeout(()=>{
                reject(new Error("timeout"));
                debug("disconnect promise timed out");
            }, timeout); 
        });
    
        this.session_message_handlers.addEventListener( "disconnect", () => {
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
           if(this.session_pending_timeouts[handler] != null)
               debug("SRSLY?");
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
    }
    
    start(){
        return this.link.send_message(new HelloMessage(), config.startup_timeout);     //Send a no-op message to launch the backend/gui
    }    
}

// Main
window.client = null;

function on_init_fail(ex){
    var msg = "Failed connecting to back-end service. The extension may not be installed properly" + ex !=null ? ex.message : "";
    notify("Failed connecting to back-end service. The extension may not be installed properly: " + msg);
}

//backend.send_message( new PinRequest(123, "https://www.facebook.com/blah/blah/blah") ).then( ()=>{
//    backend.send_message( new PinRequest(321, "https://www.facebook.com/blah/blah/blah") );
//});

async function fetch_pin_command(){
    //backend.send_message( new PinRequest(123, "https://www.facebook.com/blah/blah/blah") );
    notify("starting");

    try{    
        window.client = new Client();
        //await client.start().then( ()=>{ notify("READY"); }, on_init_fail );
        var results = client.start();
        window.client.link.consume = true;
        
        await results[0];
        debug( "HRM...:" + window.client.link.session_pending_timeouts.length );
        await results[1];
    }
    catch(ex){
        on_init_fail(ex);
    }
}

function browser_command_handler (command) {
  try{
	if (command === "oath-sign-in")
            fetch_pin_command();
	else
	  throw "Command: '" + command + "' is not recongized. Perhaps someone does us a bamboozle."; //Should not happen

  }catch(err){
	  show_error(err);
  }
}

browser.commands.onCommand.addListener(browser_command_handler); //Listen for the sign-in hotkey
//browser.runtime.onMessage.addListener(handleMessage);

