var id = "CityList";

function socketOpen() {
	// 签入
	socketSend('sign', id);
}

function socketAccept(data) {

}

function searchNow() {
	document.querySelector("#house").value = new Date();
}

function showDropdown() {
	if (document.createEvent) {
		var e = document.createEvent("MouseEvents");
		e.initMouseEvent("mousedown", true, true, window, 0, 0, 0, 0, 0, false,
				false, false, false, 0, null);
		document.querySelector("#cityList").dispatchEvent(e);
	} else if (element.fireEvent) {
		document.querySelector("#cityList").fireEvent("mousedown");
	}
}
