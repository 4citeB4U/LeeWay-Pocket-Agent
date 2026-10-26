const puppeteer=require('C:/Users/Leona/AppData/Local/npm-cache/_npx/4b4c857f6efdfb61/node_modules/puppeteer');
(async()=>{
 const browser=await puppeteer.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',args:['--no-first-run']});
 try{
  const page=await browser.newPage();
  await page.setViewport({width:1200,height:900});
  await page.goto('http://127.0.0.1:18890/ui',{waitUntil:'domcontentloaded'});
  await page.evaluate(()=>{
   document.getElementById('hamburger-btn').click();
   document.querySelector('[data-setting="vision"]').click();
  });
  await page.waitForSelector('#leeway-vision-eye-widget',{timeout:7000});
  const initial=await page.evaluate(()=>{
   const w=document.getElementById('leeway-vision-eye-widget');
   return {host:!!w,withinPanel:!!document.querySelector('#surface-panel iframe'),transparent:getComputedStyle(w).backgroundColor,menuCount:document.querySelectorAll('button[data-setting]').length,iframe:w.querySelector('iframe')?.src};
  });
  const moved=await page.evaluate(()=>{
   const w=document.getElementById('leeway-vision-eye-widget');w.style.left='145px';w.style.top='92px';w.style.width='700px';w.style.height='510px';
   return {x:w.offsetLeft,y:w.offsetTop,width:w.offsetWidth,height:w.offsetHeight};
  });
  await page.waitForFunction(()=>document.querySelector('#leeway-vision-eye-widget iframe')?.contentWindow!==null,{timeout:8000}).catch(()=>{});
  const frame=page.frames().find(f=>f.url().includes('leeway-agent-lee-vision.vercel.app'));
  const eye=frame?await frame.evaluate(()=>({title:document.title,eye:!!document.getElementById('widget'),shell:!!document.getElementById('shellMain'),camera:!!document.getElementById('camera')})).catch(e=>({error:String(e)})):null;
  await page.evaluate(()=>document.querySelector('#leeway-vision-eye-widget button[aria-label="Close Vision"]').click());
  const closed=await page.evaluate(()=>!document.getElementById('leeway-vision-eye-widget'));
  const accepted=initial.host&&!initial.withinPanel&&initial.transparent==='rgba(0, 0, 0, 0)'&&initial.menuCount===11&&moved.width===700&&closed;
  console.log(JSON.stringify({initial,moved,eye,closed,accepted},null,2));
  if(!accepted)process.exitCode=2;
 }finally{await browser.close()}
})().catch(e=>{console.error(e.stack);process.exitCode=1});
