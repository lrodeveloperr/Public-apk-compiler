from pathlib import Path
import re, json, sys

path = Path(sys.argv[1] if len(sys.argv)>1 else 'app/src/main/assets/kitchen_prep_board.html')
text = path.read_text(encoding='utf-8')

def once(old,new,label):
    global text
    if old not in text:
        raise SystemExit(f'missing patch point: {label}')
    text=text.replace(old,new,1)

def sub(pattern,repl,label,flags=re.S):
    global text
    text2,n=re.subn(pattern,lambda _m: repl,text,count=1,flags=flags)
    if n!=1:
        raise SystemExit(f'patch {label} matched {n}')
    text=text2

once('<html lang="en">','<html lang="en" data-platform="android">','html platform')
once('.topbar{height:64px;padding:0 16px;display:flex;align-items:center;justify-content:flex-start;gap:12px;background:rgba(245,240,232,.94);border-bottom:1px solid rgba(222,215,203,.78);box-shadow:0 7px 22px rgba(52,64,29,.06);position:relative;z-index:12}',
     '.topbar{height:64px;padding:0 16px;display:flex;align-items:center;justify-content:flex-start;gap:12px;background:rgba(245,240,232,.96);border-bottom:1px solid rgba(222,215,203,.78);box-shadow:0 7px 22px rgba(52,64,29,.06);position:sticky;top:0;z-index:60}',
     'sticky topbar')
once('html[data-platform="ios"] .bottomnav{padding-bottom:12px}',
     'html[data-platform="ios"] .bottomnav{padding-bottom:12px}\nhtml[data-platform="android"] .phone-status{display:none}\nhtml[data-platform="android"] .topbar{top:0}',
     'android status')
once('html[data-theme="dark"] .bottomnav button{color:#9CA095}\nhtml[data-theme="dark"] .bottomnav button.active{color:#C9D7A7}',
     'html[data-theme="dark"] .bottomnav button{color:#D8DDD1;font-weight:750}\nhtml[data-theme="dark"] .bottomnav button svg{color:#D8DDD1}\nhtml[data-theme="dark"] .bottomnav button.active{color:#DDE9B8;font-weight:950}\nhtml[data-theme="dark"] .bottomnav button.active svg{color:#DDE9B8}',
     'dark nav visibility')
once('.settings-legal .setting-link>div small{font-size:9px;color:var(--muted);font-weight:800}',
     '.settings-legal .setting-link>div small{font-size:9px;color:var(--muted);font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%}',
     'settings price nowrap')
once('.shell{max-width:var(--max);margin:0 auto;padding:0 16px 96px}',
     '.shell{max-width:var(--max);margin:0 auto;padding:0 16px calc(96px + env(safe-area-inset-bottom,0px))}',
     'shell safe bottom')
once('</style>', '.live-empty{margin-top:24px;padding:24px;text-align:center}\n.live-empty .primary{margin-top:12px}\n</style>', 'empty live css')

once('<div class="hero">','<div class="hero" id="homeHero">','hero id')
once('<div class="section">\n              <div class="sectionhead">\n                <div><h2 data-i18n="active">Active</h2></div>',
     '<div class="section" id="homeActiveSection">\n              <div class="sectionhead">\n                <div><h2 data-i18n="active">Active</h2></div>',
     'active section id')
once('<button class="start-card primary-start" data-repeat-board>', '<button class="start-card primary-start" id="homeRepeat" data-repeat-board>', 'repeat id')
once('<div class="section">\n              <div class="sectionhead"><div><h2 data-i18n="recent">Recent</h2></div><button class="linkbtn" data-view="boards" data-i18n="all">All</button></div>\n              <div class="card list">',
     '<div class="section" id="recentSection">\n              <div class="sectionhead"><div><h2 data-i18n="recent">Recent</h2></div><button class="linkbtn" data-view="boards" data-i18n="all">All</button></div>\n              <div class="card list" id="recentList">',
     'recent ids')
once('<div class="live-shell">', '<div class="card live-empty" id="liveEmpty" hidden><button class="primary" data-view="create"><span>+</span> <span data-i18n="new">New</span></button></div>\n        <div class="live-shell" id="liveShell">', 'live empty')
once('data-board-open="demo-dinner"', 'data-board-open=""', 'feature demo id')

once('<div class="setting"><div><b data-i18n="alerts">Alerts</b></div><button class="switch on" id="alertsSwitch" data-setting="alerts" data-i18n-aria="alerts"></button></div>',
     '<div class="setting"><div><b data-i18n="darkMode">Dark mode</b></div><button class="switch" id="darkModeSwitch" data-setting="darkMode" data-i18n-aria="darkMode"></button></div>\n          <div class="setting"><div><b data-i18n="alerts">Alerts</b></div><button class="switch on" id="alertsSwitch" data-setting="alerts" data-i18n-aria="alerts"></button></div>',
     'dark mode setting')

sub(r'(<div class="card form-card" id="taskList">).*?(\n\s*</div>\n\n\s*</section>\n\n\s*<section class="create-step" data-step="3">)', r'\1\2', 'empty create task list')
sub(r'(<div class="card list" id="recentList">).*?(\n\s*</div>\n\s*</div>\n\s*</div>\n\s*</section>\n\n\s*<section class="screen" id="screen-create">)', r'\1\n              </div>\n            </div>\n          </div>\n        </div>\n      </section>\n\n      <section class="screen" id="screen-create">', 'empty recent list')

m=re.search(r'const I18N=(\{.*?\});\n', text, re.S)
if not m: raise SystemExit('I18N not found')
i18n=json.loads(m.group(1))
dark={'en':'Dark mode','es':'Modo oscuro','pt':'Modo escuro','fr':'Mode sombre','de':'Dunkelmodus','it':'Modalità scura','nl':'Donkere modus','pl':'Tryb ciemny','cs':'Tmavý režim','ro':'Mod întunecat','hu':'Sötét mód','sv':'Mörkt läge','da':'Mørk tilstand','nb':'Mørk modus','fi':'Tumma tila','tr':'Koyu mod','ar':'الوضع الداكن','he':'מצב כהה','hi':'डार्क मोड','bn':'ডার্ক মোড','pa':'ਡਾਰਕ ਮੋਡ','id':'Mode gelap','ms':'Mod gelap','fil':'Madilim na mode','vi':'Chế độ tối','th':'โหมดมืด','ja':'ダークモード','ko':'다크 모드','zh-Hans':'深色模式','zh-Hant':'深色模式','ru':'Тёмная тема'}
for code,pack in i18n.items(): pack['s']['darkMode']=dark.get(code,'Dark mode')
text=text[:m.start(1)]+json.dumps(i18n,ensure_ascii=False,separators=(',',':'))+text[m.end(1):]

once('function applyLocaleFormatting(code){', 'let createStep=1;\n\nfunction applyLocaleFormatting(code){', 'early createStep')
once('const INITIAL_TASK_LIST_MARKUP=document.getElementById("taskList")?.innerHTML||"";\nlet createStep=1;', 'const INITIAL_TASK_LIST_MARKUP="";', 'remove late createStep')

sub(r'document\.getElementById\("addTask"\)\.onclick=\(\)=>\{.*?\n\};\ndocument\.querySelectorAll\("#timingSeg button"\)', '''function appendDraftTask(name=""){
  const list=document.getElementById("taskList");if(!list)return null;
  const d=document.createElement("div");d.className="task-card";
  d.innerHTML='<span class="drag">⠿</span><div class="thumb"><svg viewBox="0 0 48 48"><rect width="48" height="48" fill="#E7E2D9"/><circle cx="24" cy="24" r="10" fill="#89916A"/></svg></div><div class="grow"><div class="task-name" contenteditable="true" role="textbox"></div><div class="task-meta" data-i18n="edit"></div></div><span class="badge" data-duration-minutes="10">10</span>';
  const nameEl=d.querySelector(".task-name");nameEl.textContent=name||I18N[uiLocale].s.newTask;
  list.appendChild(d);applyLocaleFormatting(uiLocale);return d;
}
document.getElementById("addTask").onclick=()=>{const d=appendDraftTask("");d?.querySelector(".task-name")?.focus();};
document.querySelectorAll("#timingSeg button")''', 'add task editable')

sub(r'function seedBoards\(\)\{.*?\n\}', 'function seedBoards(){ return []; }', 'seed boards')
sub(r'function safeLoadBoards\(\)\{.*?\n\}\nlet boards=safeLoadBoards\(\);', '''function safeLoadBoards(){
  try{
    const raw=localStorage.getItem(BOARDS_KEY);
    if(!raw)return [];
    const parsed=JSON.parse(raw);
    if(!Array.isArray(parsed))return [];
    const clean=parsed.map(normalizeBoard).filter(b=>!String(b.id).startsWith("demo-"));
    if(clean.length!==parsed.length)localStorage.setItem(BOARDS_KEY,JSON.stringify(clean));
    return clean;
  }catch(_){return []}
}
let boards=safeLoadBoards();''', 'safe boards')

once('  const feature=getBoard(typeof session!=="undefined"?session.activeBoardId:null)||getPreferredBoard();\n  if(feature){',
'''  const feature=getBoard(typeof session!=="undefined"?session.activeBoardId:null)||getPreferredBoard();
  const featureEl=document.getElementById("featureBoard");if(featureEl)featureEl.hidden=!feature;
  const recentList=document.getElementById("recentList");
  if(recentList){
    recentList.replaceChildren();
    boards.filter(b=>b.status==="completed").sort((a,b)=>b.createdAt-a.createdAt).slice(0,3).forEach(b=>recentList.appendChild(renderBoardRow(b)));
  }
  const recentSection=document.getElementById("recentSection");if(recentSection)recentSection.hidden=!(recentList&&recentList.children.length);
  if(feature){''', 'feature fresh state')

sub(r'function taskLaneSeed\(board\)\{.*?\n\}', '''function taskLaneSeed(board){
  const map={};
  (board?.tasks||[]).forEach((t,i)=>map[t.id]=["now","waiting","next","done"].includes(t.initialLane)?t.initialLane:(i<2?"now":"next"));
  return map;
}''', 'task lane seed')
sub(r'function taskTimerSeed\(board,now=Date\.now\(\)\)\{.*?\n\}', '''function taskTimerSeed(board,now=Date.now()){
  const timers={};
  (board?.tasks||[]).forEach(t=>{
    const lane=t.initialLane;
    const seconds=Math.max(0,Number(t.durationSeconds)||Number(t.durationMinutes)*60||0);
    if(seconds>0&&(lane==="now"||lane==="waiting"))timers[t.id]={remaining:seconds,targetEpoch:now+seconds*1000};
  });
  return timers;
}''', 'task timer seed')

once('const loadedSession=safeLoadSession();\nconst fallbackBoard=getBoard(loadedSession?.activeBoardId)||getPreferredBoard();\nlet session=loadedSession\n  ?Object.assign(makeSession(loadedSession.currentView||"home",fallbackBoard,loadedSession.settings),loadedSession)\n  :makeSession("home",fallbackBoard);',
'''const loadedSession=safeLoadSession();
const validLoadedSession=loadedSession&&getBoard(loadedSession.activeBoardId)?loadedSession:null;
const fallbackBoard=getBoard(validLoadedSession?.activeBoardId)||getPreferredBoard();
let session=validLoadedSession
  ?Object.assign(makeSession(validLoadedSession.currentView||"home",fallbackBoard,validLoadedSession.settings),validLoadedSession)
  :makeSession("home",fallbackBoard);''', 'valid session only')
once('session.taskLanes=Object.assign(taskLaneSeed(activeBoardAtBoot),loadedSession?.taskLanes||session.taskLanes||{});', 'session.taskLanes=Object.assign(taskLaneSeed(activeBoardAtBoot),validLoadedSession?.taskLanes||session.taskLanes||{});', 'session lanes')
once('session.settings=Object.assign({alerts:true,awake:true,compact:false,haptics:true},session.settings||{});', 'session.settings=Object.assign({alerts:true,awake:true,compact:false,haptics:true,darkMode:null},session.settings||{});', 'settings dark default')
once('session.taskTimers=Object.assign(taskTimerSeed(activeBoardAtBoot),loadedSession?.taskTimers||session.taskTimers||{});', 'session.taskTimers=Object.assign(taskTimerSeed(activeBoardAtBoot),validLoadedSession?.taskTimers||session.taskTimers||{});', 'session timers')
once('settings:Object.assign({alerts:true,awake:true,compact:false,haptics:true},settings||{})', 'settings:Object.assign({alerts:true,awake:true,compact:false,haptics:true,darkMode:null},settings||{})', 'make session dark')

sub(r'function renderLiveTasksFromBoard\(board\)\{.*?\n\}', '''function renderLiveTasksFromBoard(board){
  document.querySelectorAll("#lane-now > .live-card,#lane-waiting > .live-card,#lane-next > .live-card").forEach(x=>x.remove());
  if(doneItems)doneItems.replaceChildren();
  const shell=document.getElementById("liveShell"),empty=document.getElementById("liveEmpty");
  if(shell)shell.hidden=!board;if(empty)empty.hidden=!!board;
  if(!board)return;
  (board.tasks||[]).forEach(task=>{
    const lane=["now","waiting","next","done"].includes(task.initialLane)?task.initialLane:"next";
    const card=liveTaskCard(task,lane);
    const dest=lane==="done"?doneItems:document.getElementById(`lane-${lane}`);
    if(dest)dest.appendChild(card);
  });
}''', 'live renderer')

once('function renderActiveBoardContext(){\n  const board=currentBoard();if(!board)return;',
'''function renderActiveBoardContext(){
  const board=currentBoard();
  const hero=document.getElementById("homeHero"),active=document.getElementById("homeActiveSection"),repeat=document.getElementById("homeRepeat");
  if(hero)hero.hidden=!board;if(active)active.hidden=!board;if(repeat)repeat.hidden=!board;
  if(!board){updateWaitingAlert();return;}''', 'home fresh context')

sub(r'document\.querySelectorAll\("\[data-paste-start\]"\)\.forEach\(btn=>btn\.addEventListener\("click",\(\)=>\{.*?\n\}\)\);', '''async function readPasteText(){
  try{if(window.AndroidBridge&&typeof AndroidBridge.getClipboardText==="function")return String(AndroidBridge.getClipboardText()||"");}catch(_){}
  try{if(navigator.clipboard?.readText)return await navigator.clipboard.readText();}catch(_){}
  return "";
}
document.querySelectorAll("[data-paste-start]").forEach(btn=>btn.addEventListener("click",async()=>{
  prepareNewBoardDraft();createStep=2;setView("create");renderCreateStep();
  const raw=await readPasteText();
  const lines=raw.split(/\\r?\\n/).map(x=>x.replace(/^[-*•\\d.)\\s]+/,"").trim()).filter(Boolean);
  if(lines.length){const list=document.getElementById("taskList");if(list)list.replaceChildren();lines.slice(0,100).forEach(line=>appendDraftTask(line));}
}));''', 'paste handler')

once('  session.settings[sw.dataset.setting]=sw.classList.contains("on");\n  if(sw.dataset.setting==="compact")document.body.classList.toggle("compact-live",session.settings.compact);\n  saveSession();syncWakeLock();',
'''  session.settings[sw.dataset.setting]=sw.classList.contains("on");
  if(sw.dataset.setting==="compact")document.body.classList.toggle("compact-live",session.settings.compact);
  if(sw.dataset.setting==="darkMode")applyThemePreference();
  saveSession();syncWakeLock();''', 'theme handler')

sub(r'function applyPreviewState\(state\)\{.*?\n\}', '''let kpbSystemTheme="light";
function applyThemePreference(){
  const chosen=typeof session?.settings?.darkMode==="boolean"?(session.settings.darkMode?"dark":"light"):kpbSystemTheme;
  document.documentElement.dataset.theme=chosen;
  const sw=document.getElementById("darkModeSwitch");if(sw)sw.classList.toggle("on",chosen==="dark");
}
function applyPreviewState(state){
  kpbSystemTheme=(state&&state.theme)==="dark"?"dark":"light";
  const platform=(state&&state.platform)==="ios"?"ios":"android";
  document.documentElement.dataset.platform=platform;
  applyThemePreference();
  const status=document.querySelector(".phone-status");
  if(status){
    status.innerHTML=platform==="ios"
      ? '<span>9:41</span><span>● 5G ▰</span>'
      : '<span>9:30</span><span>● 5G ▴</span>';
  }
}''', 'preview theme')

path.write_text(text,encoding='utf-8')
print('patched', path, 'bytes', len(text.encode('utf-8')))
