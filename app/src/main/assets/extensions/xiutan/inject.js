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
function ready(fn) {
  if (document.readyState != 'loading') {
    fn();
  } else {
    document.addEventListener('DOMContentLoaded', fn);
  }
}
ready(function() {
  let link = document.querySelector('link[rel="shortcut icon"]');
  if(link == null) {
      let iconLinks = document.querySelectorAll('link[rel="icon"]') || [];
      let maxSize = 0;
      let maxLink = iconLinks.length > 0 ? iconLinks[0] : null;
      iconLinks.forEach(link => {
        try {
            let sizes = link.getAttribute('sizes');
            if (sizes) {
              let sizeArray = sizes.split('x');
              let width = parseInt(sizeArray[0]);
              let height = parseInt(sizeArray[1]);
              let area = width * height;
              if (area > maxSize) {
                maxSize = area;
                maxLink = link;
              }
            }
        } catch(e) {
            console.log(e);
        }
      });
      if(maxLink != null) {
        link = maxLink;
      }
  }
  if(link == null) {
      link = document.querySelector('link[rel="apple-touch-icon"]');
  }
  if (link && link.href) {
      console.log('favicon found: ' + link.href);
      let faviconUrl = new URL(link.href, document.location.href).href;
      browser.runtime.sendMessage({type: "favicon", faviconUrl: faviconUrl, url: document.location.href});
  } else {
      console.log('No favicon found');
  }
});