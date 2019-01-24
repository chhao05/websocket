var MutationObserver = window.MutationObserver || window.WebKitMutationObserver
		|| window.MozMutationObserver;
var list = document.querySelector('.content');

var observer = new MutationObserver(function(mutations) {
	mutations.forEach(function(mutation) {
		console.log(mutation)
//		if (mutation.type === 'childList') {
//			var list_values = [].slice.call(list.children).map(function(node) {
//				return node.innerHTML;
//			}).filter(function(s) {
//				if (s === '<br>') {
//					return false;
//				} else {
//					return true;
//				}
//			});
//			console.log(list_values);
//		}
	});
});

observer.observe(list, {
	attributes : true,
	childList : true,
	characterData : true,
	subtree:true
});


function addli(){
	
}