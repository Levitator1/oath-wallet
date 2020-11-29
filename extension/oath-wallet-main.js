//Haven't found a good way to cenralize this string, so it has to be set both here
//and in manifest.json. It does have to be consistent in order to work.
var extension_id = "oath-wallet@Levitat0r";
var extension_name = "oath-wallet";
var pin_service_name = "oath-wallet-service";

console.log("OATH Wallet started" );

function notify(title, message){
	browser.notifications.create({
		"type": "basic",
		"title": title,
		"message": message
	});
}

notify("More Initialization Woots", "MORE WOOTS");
:
function fetch_pin_command_failure(message){
	notify("Unexpected error retrieving active browser window: " + message);
}

function fetch_pin_for_window(window){
	var tab = browser.tabs.getCurrent();
	console.log(JSON.stringify(window));
	notify("DEBUG", "The window location is: " + tab.url);
}

function fetch_pin_for_tab(tab){
	console.log("tab url is: " + tab.url);
	console.log(JSON.stringify(tab));
}

function do_throw(err){
	throw err;
}

function show_error(err){
	var msg = "Unexpected error fetching PIN: " + err;
	notify("OATH Wallet Error", msg);
	console.log(msg);
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
	console.log("Sent OATH Wallet PIN request to extension");
`

//Have to remember from which tab and url we are anticipating a reply to keep
//evildoers from spoofing replies
class PinRequest {
	constructor( tab_in ){
		this.tab = tab_in;
		this.url = tab_in.url;
	}
}

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
		console.log( "Dropped spurious (suspicious) message from: '" + sender.url);
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

async function fetch_pin_command(){

	//PIN request function is not reentrant. One request at a time, plz.
	if(pin_request_state != null){
		notify(`${extension_id} Warning', "Ignored a PIN request because another request is pending`);
		return;
	}

	tab = await get_active_tab();
	console.log("Processing PIN request for site: " + tab.url);


}

async function browser_command_handler (command) {
  try{
	if (command === "oath-sign-in")
		await fetch_pin_command();
	else
	  throw "Command: '" + command + "' is not recongized. Perhaps someone does us a bamboozle."; //Should not happen

  }catch(err){
	  show_error(err);
  }
}

// Main
browser.commands.onCommand.addListener(browser_command_handler);
browser.runtime.onMessage.addListener(handleMessage);

