const puppeteer=require('C:/Users/Leona/AppData/Local/npm-cache/_npx/4b4c857f6efdfb61/node_modules/puppeteer');
(async()=>{
 const browser=await puppeteer.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',args:['--no-first-run','--no-default-browser-check']});
 try{
  const page=await browser.newPage();
  await page.goto('http://127.0.0.1:18890/ui',{waitUntil:'domcontentloaded',timeout:15000});
  await page.waitForSelector('[data-setting="vision"]');
  const original=await page.$$eval('button[data-setting]',els=>els.map(e=>e.dataset.setting));
  await page.click('#hamburger-btn');
  await page.click('[data-setting="vision"]');
  await new Promise(r=>setTimeout(r,1700)); const frame=page.frames().find(f=>f.url().includes('leeway-agent-lee-vision.vercel.app')); const visionDocument=frame?await frame.evaluate(()=>({title:document.title,ready:document.readyState,cameraControls:[...document.querySelectorAll('button')].some(b=>b.textContent.includes('START LOCAL CAMERA'))})).catch(e=>({error:String(e)})):null;
  const opened=await page.evaluate(()=>({open:document.getElementById('surface-panel').classList.contains('open'),src:document.querySelector('#surface-panel iframe')?.getAttribute('src'),menuCount:document.querySelectorAll('button[data-setting]').length}));
  await page.evaluate(()=>document.querySelector('#surface-panel button')?.click());
  const closed=await page.evaluate(()=>!document.getElementById('surface-panel').classList.contains('open'));
  console.log(JSON.stringify({visionDocument,source:'live Windows :18890/ui',menuKeys:original,opened,closed,accepted:original.includes('vision')&&opened.open&&closed},null,2));
 }finally{await browser.close()}
})().catch(e=>{console.error(String(e.stack||e));process.exit(1)});
