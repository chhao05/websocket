document.addEventListener('DOMContentLoaded', function() {
	// 监听端的事件被动触发，不进行监听
	if (self != top) {
		return;
	}

	/*
	 * 冒泡事件通过代理对象来捕获 : 
	 * MouseEvent:'click','dblclick','mouseup','mousedown'
	 * FocusEvent:'focusin','focusout' 
	 * InputEvent:'input' Event:'change'
	 */
	var BUBBLE_EVENTS = [ 'click', 'input', 'change' ];
	BUBBLE_EVENTS.forEach(function(item) {
		document.body.addEventListener(item, function(e) {
			compressEvent(e);
		});
	})

	/*
	 * 非冒泡事件在元素自身捕获 : 
	 * FocusEvent:'focus','blur'
	 */
	var NO_BUBBLE_EVENTS = [ 'focus', 'blur' ];
	if (NO_BUBBLE_EVENTS.length) {
		document.querySelectorAll('input,textarea').forEach(function(target) {
			NO_BUBBLE_EVENTS.forEach(function(type) {
				target.addEventListener(type, function(e) {
					compressEvent(e);
				})
			})
		})
	}

}, false);

// 定义事件中无需进行网络传输的属性
var USELESS_ATTRS = [ 'isTrusted', 'timeStamp', 'isComposing', 'detail',
		'eventPhase', 'which', 'defaultPrevented', 'composed', 'returnValue',
		'cancelBubble' ];
/**
 * 事件封装处理
 * 
 * @param e
 * @returns
 */
function compressEvent(e) {
	var event = {};
	for (variable in e) {
		var type = typeof e[variable];
		if (type === 'number' || type === 'boolean' || type === 'string') {
			// 剔除无用属性及常量属性（大写单词或以下划线链接的大写单词变量）
			if (!arrayContains(USELESS_ATTRS, variable)
					&& !/^[A-Z_]+$/.test(variable)) {
				event[variable] = e[variable];
			}
		}
	}
	// 获取event元素选择器
	event.selectors = buildSelectors(e);
	// 获取事件的构建函数名称
	event.constructor = e.constructor.name;
	// 元素支持value属性，则进行赋值
	if (typeof e.target.value) {
		event.value = e.target.value;
	}
	// socket传播事件
	socketSend('event', JSON.stringify(event));
}

/**
 * 判断集合中是否包含指定元素
 * 
 * @param array
 *            集合
 * @param item
 *            被包含元素
 * @returns
 */
function arrayContains(array, item) {
	if (array instanceof Array) {
		for (var i = 0, j = array.length; i < j; i++) {
			if (item === array[i]) {
				return true;
			}
		}
	}
	return false;
}

/**
 * 
 * @param event
 *            事件源
 * @returns event.target选择器
 */
function buildSelectors(event) {
	// 优先id选择器
	if (event.target.id) {
		var tagName = event.target.tagName;
		if (tagName) {
			return tagName.toLowerCase() + '#' + event.target.id;
		}else{
			return  '#' + event.target.id;
		}
	}
	var selectors = '';
	// path保存事件的传播路径
	var path = event.path || (event.composedPath && event.composedPath());
	if (path && path instanceof Array) {
		// 最多取三级传播路径
		if (path.length > 3) {
			path = path.slice(0, 3);
		}
		path.forEach(function(item) {
			selectors = getElementSelectors(item) + ' ' + selectors;
		})
	} else {
		selectors = getElementSelectors(event.parentElement) + ' '
				+ getElementSelectors(event.target);
	}
	return selectors.trim();
}

/**
 * 获取元素属性选择器
 * 
 * @param target
 * @returns
 */
function getElementSelectors(target) {
	var selectors = '';
	// 标签选择器
	if (target.tagName) {
		selectors += target.tagName.toLowerCase();
	}
	// name选择器
	if (target.name) {
		selectors += '[name=\'' + target.name + '\']'
	}
	// class选择器
	if (target.className) {
		var classArray = target.className.split(/\s+/);
		classArray.forEach(function(item) {
			selectors += '.' + item
		})
	}
	return selectors;
}

/** ***************************************供父页面事件回调********************************** */
/** ************************************************************************************ */
/**
 * 事件处理
 * 
 * @param jsonStr
 * @returns
 */
function eventHandle(jsonStr) {
	// document事件对象初始化属性
	var eventInit = JSON.parse(jsonStr);
	// 事件源
	var target = document.querySelector(eventInit.selectors);
	// 模拟用户操作
	simulateOperation(target, eventInit);
	// 判断当前浏览器是否支持该事件，进行事件发布
	if (window[eventInit.constructor]) {
		var type = eventInit.type;
		var constructor = eventInit.constructor;
		var event = new window[constructor](type, eventInit);
		target.dispatchEvent(event);
	} else {
		console && console.warn('当前浏览器不支持该事件：' + eventInit.constructor)
	}
}

/**
 * 还原用户的一些操作
 * 
 * @param eventInit
 * @returns
 */
function simulateOperation(target, eventInit) {
	// 支持value属性则进行赋值，输入回显
	if (typeof eventInit.value) {
		target.value = eventInit.value;
	}
	// 焦点事件
	if (eventInit.constructor === 'FocusEvent') {
		if (target[eventInit]) {
			target[eventInit]();
		}
	}
}
