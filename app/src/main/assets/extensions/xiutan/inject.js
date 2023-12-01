browser.runtime.onMessage.addListener((message) => {
  console.log('Received message from background:', message);
  if(message.type === 'input') {
        function isInputInUpperHalf(element) {
            if(element == null) {
                console.log("showSoftInput: element is null");
                return false;
            }
            console.log("showSoftInput: element is not null");
            const rect = element.getBoundingClientRect();
            const viewportHeight = window.innerHeight;
            const screenCenterY = viewportHeight / 2;
            return rect.top + rect.height < screenCenterY;
        }
        let ele = document.querySelector('input[type^="text"]:focus, input[type^="number"]:focus, input[type^="password"]:focus, input[type^="search"]:focus, input[type^="tel"]:focus, input[type^="email"]:focus, input[type^="date"]:focus, input[type^="time"]:focus, input[type^="url"]:focus, textarea:focus, input:not([type]):focus');
        let isUp = isInputInUpperHalf(ele);
        browser.runtime.sendMessage({type: message.type, isUp: isUp});
  }
});