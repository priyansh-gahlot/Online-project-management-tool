'use strict';
const base=document.querySelector('meta[name="app-context"]').content;
const csrf=document.querySelector('meta[name="csrf-token"]').content;
const main=document.getElementById('main'),modal=document.getElementById('modal');
let state,search='',statusFilter='',projectFilter='',onlyMine=false,taskView='board',toastTimer;
const statuses=['TODO','IN_PROGRESS','REVIEW','COMPLETED'],projectStatuses=['PLANNING','IN_PROGRESS','ON_HOLD','COMPLETED'];
const $=s=>document.querySelector(s);
const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const words=v=>String(v||'').toLowerCase().replaceAll('_',' ').replace(/\b\w/g,c=>c.toUpperCase());
const initials=name=>String(name||'?').split(' ').map(n=>n[0]).slice(0,2).join('').toUpperCase();
const today=()=>{const d=new Date();return new Date(d.getFullYear(),d.getMonth(),d.getDate());};
const days=date=>date?Math.round((new Date(String(date).slice(0,10)+'T00:00:00')-today())/86400000):null;
const due=date=>date?new Date(String(date).slice(0,10)+'T00:00:00').toLocaleDateString('en',{month:'short',day:'numeric'}):'No deadline';
const ago=date=>{const d=new Date(String(date).replace(' ','T'));const n=Math.max(0,Math.floor((new Date()-d)/60000));return n<1?'Just now':n<60?n+'m ago':n<1440?Math.floor(n/60)+'h ago':Math.floor(n/1440)+'d ago';};
const project=id=>state.projects.find(p=>p.id===Number(id));
const tasksFor=id=>state.tasks.filter(t=>t.project_id===Number(id));
const membersFor=id=>state.members.filter(m=>m.project_id===Number(id));
const canCreate=()=>['ADMIN','PROJECT_MANAGER'].includes(state.user.role);
const progress=id=>{const ts=tasksFor(id);return ts.length?Math.round(ts.filter(t=>t.status==='COMPLETED').length/ts.length*100):0;};
const overdue=t=>t.status!=='COMPLETED'&&days(t.deadline)!==null&&days(t.deadline)<0;
const score=t=>({LOW:1,MEDIUM:2,HIGH:3,URGENT:4}[t.priority]*10+(days(t.deadline)===null?0:Math.max(0,14-days(t.deadline))*3));
const avatar=(name,extra='')=>'<span class="avatar '+extra+'" title="'+esc(name)+'">'+esc(initials(name))+'</span>';
const badge=s=>'<span class="badge '+esc(s.toLowerCase())+'">'+words(s)+'</span>';
const empty=(text,action='')=>'<div class="empty">'+text+(action?'<br>'+action:'')+'</div>';
const button=(action,id,label,cls='')=>'<button class="btn '+cls+'" data-action="'+action+'" data-id="'+id+'">'+label+'</button>';
const field=(label,name,value='',type='text',required=false,full=false)=>'<label class="field '+(full?'full':'')+'">'+label+'<input name="'+name+'" type="'+type+'" value="'+esc(value)+'" '+(required?'required ':'')+(type==='text'?'maxlength="255"':'')+'></label>';
const select=(label,name,values,value,full=false)=>'<label class="field '+(full?'full':'')+'">'+label+'<select name="'+name+'">'+values.map(v=>'<option value="'+esc(Array.isArray(v)?v[0]:v)+'" '+(String(Array.isArray(v)?v[0]:v)===String(value)?'selected':'')+'>'+esc(Array.isArray(v)?v[1]:words(v))+'</option>').join('')+'</select></label>';
const textarea=(label,name,value='',max=5000)=>'<label class="field full">'+label+'<textarea name="'+name+'" maxlength="'+max+'">'+esc(value)+'</textarea></label>';
function notify(text,error=false){const el=$('#toast');el.textContent=text;el.className=error?'error':'';el.style.display='block';clearTimeout(toastTimer);toastTimer=setTimeout(()=>el.style.display='none',4500);}
async function api(action,values){
 const response=await fetch(base+'/api/'+action,{method:values?'POST':'GET',headers:values?{'X-CSRF-Token':csrf,'Content-Type':'application/x-www-form-urlencoded'}:{},body:values?new URLSearchParams(values):undefined});
 if(response.status===401){location.href=base+'/login.jsp';throw Error('Please sign in again.');}
 const data=await response.json();if(!response.ok)throw Error(data.error||'Request failed.');return data;
}
async function refresh(){state=await api('workspace');render();}
async function save(action,values,message='Changes saved',close=true){await api(action,values);await refresh();if(close)modal.close();notify(message);}
function heading(title,subtitle,action=''){return '<div class="page-heading"><div><div class="eyebrow">YOUR TEAM, IN SYNC</div><h1>'+title+'</h1><p class="subtitle">'+subtitle+'</p></div>'+action+'</div>';}
function metric(label,value,note,icon,green=false){return '<div class="metric"><div class="metric-top">'+label+'<span class="metric-icon">'+icon+'</span></div><div class="metric-value">'+value+'</div><div class="metric-note '+(green?'green':'')+'">'+note+'</div></div>';}
function metrics(){
 const completed=state.tasks.filter(t=>t.status==='COMPLETED').length;
 return '<div class="metrics">'+metric('Total projects',state.projects.length,state.projects.filter(p=>p.status==='IN_PROGRESS').length+' currently in progress','▣')+metric('Open tasks',state.tasks.length-completed,'Every next step, in one place','☷')+metric('Completed tasks',completed,(state.tasks.length?Math.round(completed/state.tasks.length*100):0)+'% of all tasks complete','✓',true)+metric('Overdue tasks',state.tasks.filter(overdue).length,'A little attention goes a long way','◷')+'</div>';
}
function projectCard(p,i){
 const members=membersFor(p.id),percent=progress(p.id);
 return '<button class="project-card" data-action="openProject" data-id="'+p.id+'"><div class="card-top"><span class="project-icon c'+i%3+'">'+['▧','◇','▤'][i%3]+'</span>'+badge(p.status)+'</div><h3>'+esc(p.name)+'</h3><p>'+esc(p.description||'Your next great project starts here.')+'</p><div class="progress-label"><span>Progress</span><strong>'+percent+'%</strong></div><div class="progress"><span style="width:'+percent+'%"></span></div><div class="card-footer"><div class="avatars">'+members.slice(0,4).map(m=>avatar(m.full_name)).join('')+(members.length>4?avatar('+'+(members.length-4)):'')+'</div><span class="deadline">◷ '+due(p.deadline)+'</span></div></button>';
}
function taskTable(tasks){
 return tasks.length?'<div class="table-wrap"><table><thead><tr><th>TASK NAME</th><th>PRIORITY</th><th>DUE DATE</th><th>STATUS</th></tr></thead><tbody>'+tasks.map(t=>'<tr><td class="task-title"><button data-action="task" data-id="'+t.id+'">'+esc(t.title)+'</button><small>'+esc(t.project_name)+'</small></td><td><span class="priority '+t.priority.toLowerCase()+'">'+words(t.priority)+'</span></td><td class="'+(overdue(t)?'overdue':'')+'">'+due(t.deadline)+'</td><td>'+badge(t.status)+'</td></tr>').join('')+'</tbody></table></div>':empty('No tasks here yet. A clear list is a fresh start.');
}
function activities(){
 return state.activity.length?state.activity.slice(0,6).map(a=>'<div class="activity-item"><span class="activity-dot"></span><div><p><strong>'+esc(a.full_name||'Team member')+'</strong><br>'+esc(a.action)+'</p><small>'+esc(a.project_name)+' · '+ago(a.created_at)+'</small></div></div>').join(''):empty('Team activity will appear as you make progress.');
}
function dashboard(){
 const name=state.user.full_name.split(' ')[0],pending=state.tasks.filter(t=>t.status!=='COMPLETED').sort((a,b)=>score(b)-score(a));
 return heading('Let’s make progress, '+esc(name)+'.','A little clarity for everything your team is working on.',canCreate()?button('newProject','','＋ New project','primary'):'')+
 '<section class="hero"><div><span class="hero-tag">✦ A FRESH PERSPECTIVE</span><h2>Big ideas. Small steps. Real progress.</h2><p>Your projects, people, and priorities — brought together in one thoughtful workspace.</p></div><div class="hero-art" aria-hidden="true"><div class="art-card back"></div><div class="art-card"></div><span class="art-spark">✦</span></div></section>'+
 metrics()+'<div class="section-title"><h2>Your projects <span style="color:#a3ada0;font-size:12px;font-weight:400">('+state.projects.length+')</span></h2><a class="link" href="#projects">View all projects ↗</a></div><div class="projects-grid">'+(state.projects.length?state.projects.slice(0,3).map(projectCard).join(''):empty('No projects yet.',canCreate()?button('newProject','','Create your first project','primary'):'Your project manager can invite you to a project.'))+'</div>'+
 '<div class="two-col"><section class="panel"><div class="section-title"><h2>What needs your attention</h2><a class="link" href="#tasks">All tasks ↗</a></div>'+taskTable(pending.slice(0,5))+'</section><section class="panel"><div class="section-title"><h2>Around the workspace</h2><span class="badge">Latest activity</span></div>'+activities()+'</section></div>';
}
function projectList(){
 const list=state.projects.filter(p=>(!search||(p.name+' '+p.description).toLowerCase().includes(search.toLowerCase()))&&(!statusFilter||p.status===statusFilter));
 return heading('A home for every project.','Bring your plans into focus and keep the whole team moving.',canCreate()?button('newProject','','＋ New project','primary'):'')+
 '<div class="toolbar"><input id="search" class="search" aria-label="Search projects" placeholder="⌕  Search projects…" value="'+esc(search)+'"><select id="status-filter" class="filter" aria-label="Filter project status"><option value="">All statuses</option>'+projectStatuses.map(s=>'<option '+(statusFilter===s?'selected':'')+' value="'+s+'">'+words(s)+'</option>').join('')+'</select></div><div class="projects-grid">'+(list.length?list.map(projectCard).join(''):empty('No projects match these filters.'))+'</div>';
}
function taskCard(t){
 return '<button class="task-card" data-action="task" data-id="'+t.id+'"><span class="priority '+t.priority.toLowerCase()+'">'+words(t.priority)+' priority</span><h3>'+esc(t.title)+'</h3><p>'+esc(t.project_name)+'</p><div class="task-card-foot"><span class="'+(overdue(t)?'overdue':'')+'">◷ '+due(t.deadline)+'</span>'+avatar(t.assignee_name||'Unassigned')+'</div></button>';
}
function taskBoard(tasks){
 return '<div class="board">'+statuses.map(s=>{const list=tasks.filter(t=>t.status===s);return '<section class="column"><h3>'+words(s)+' <span>'+list.length+'</span></h3>'+list.map(taskCard).join('')+(list.length?'':'<div class="empty">Room for your next step.</div>')+'</section>';}).join('')+'</div>';
}
function taskList(){
 const list=state.tasks.filter(t=>(!search||(t.title+' '+t.project_name).toLowerCase().includes(search.toLowerCase()))&&(!statusFilter||t.status===statusFilter)&&(!projectFilter||t.project_id===Number(projectFilter))&&(!onlyMine||t.assigned_to===state.user.id)).sort((a,b)=>score(b)-score(a));
 return heading('From to-do to done.','A shared view of the work ahead. Open any task to update it or leave a comment.',state.projects.some(p=>p.can_manage)?button('newTask','','＋ New task','primary'):'')+
 '<div class="toolbar"><div class="filters"><input id="search" class="search" aria-label="Search tasks" placeholder="⌕  Search tasks…" value="'+esc(search)+'"><select id="project-filter" class="filter" aria-label="Filter by project"><option value="">All projects</option>'+state.projects.map(p=>'<option value="'+p.id+'" '+(String(p.id)===projectFilter?'selected':'')+'>'+esc(p.name)+'</option>').join('')+'</select><select id="status-filter" class="filter" aria-label="Filter task status"><option value="">All statuses</option>'+statuses.map(s=>'<option value="'+s+'" '+(s===statusFilter?'selected':'')+'>'+words(s)+'</option>').join('')+'</select><label class="btn"><input id="only-mine" type="checkbox" '+(onlyMine?'checked':'')+'> Assigned to me</label></div><div class="view-toggle">'+button('board','','▥ Board',taskView==='board'?'active':'')+button('list','','☷ List',taskView==='list'?'active':'')+'</div></div>'+
 (list.length?(taskView==='board'?taskBoard(list):'<div class="panel">'+taskTable(list)+'</div>'):empty('No tasks match these filters.'));
}
function projectDetail(id){
 const p=project(id);if(!p)return empty('Project not found or access is no longer available.','<a class="link" href="#projects">Return to projects</a>');
 const tasks=tasksFor(id),members=membersFor(id),pct=progress(id);
 return '<div class="detail-header"><span class="project-icon">▧</span><div><p class="eyebrow">PROJECT WORKSPACE</p><h1>'+esc(p.name)+'</h1><p>'+esc(p.description)+'</p>'+badge(p.status)+'</div><div class="detail-actions">'+(p.can_manage?button('editProject',id,'Edit project')+button('newTask',id,'＋ New task','primary'):'')+'</div></div>'+
 '<div class="detail-tabs"><strong>Overview & tasks</strong><span>'+members.length+' team members</span><span>Due '+due(p.deadline)+'</span></div>'+
 '<div class="metrics">'+metric('Total tasks',tasks.length,'Every step of the project','☷')+metric('Progress',pct+'%',tasks.filter(t=>t.status==='COMPLETED').length+' completed tasks','✓',true)+metric('Team members',members.length,'Working better together','♧')+metric('Overdue tasks',tasks.filter(overdue).length,'Keep an eye on deadlines','◷')+'</div>'+
 '<div class="two-col"><section><div class="section-title"><h2>Project tasks</h2></div><div class="panel">'+taskTable(tasks)+'</div></section><section class="panel"><div class="section-title"><h2>The team</h2>'+(p.can_manage?button('invite',id,'＋ Add member'):'')+'</div>'+members.map(m=>'<div class="member-row">'+avatar(m.full_name)+'<div><strong>'+esc(m.full_name)+'</strong><small>'+words(m.project_role)+'</small></div>'+(p.can_manage&&m.project_role!=='OWNER'?'<button class="link" data-action="removeMember" data-id="'+m.user_id+'" data-project="'+id+'">Remove</button>':'')+'</div>').join('')+'</section></div>';
}
function team(){
 const people=new Map();state.members.forEach(m=>people.set(m.user_id,m));const list=[...people.values()];
 return heading('Good work starts with people.','A clear picture of your team’s capacity across accessible projects.')+
 '<div class="report-grid"><section class="panel"><div class="section-title"><h2>Team workload</h2><span class="badge">'+list.length+' collaborators</span></div>'+ (list.map(m=>{
  const tasks=state.tasks.filter(t=>t.assigned_to===m.user_id&&t.status!=='COMPLETED'),late=tasks.filter(overdue).length;
  return '<div class="workload-row"><div class="workload-label"><span>'+esc(m.full_name)+'</span><small>'+tasks.length+' open · '+late+' overdue</small></div><div class="progress"><span style="width:'+Math.min(100,tasks.length/5*100)+'%;background:'+(tasks.length>5?'#cc9b6a':'#86a575')+'"></span></div></div>';
 }).join('')||empty('Add members to a project to see team capacity.'))+'<div class="insight">Capacity guide: up to 5 open tasks per person is balanced. More than 5 suggests a workload review. Counts include only projects you can access.</div></section><section class="panel"><h2>Your collaborators</h2>'+list.map(m=>'<div class="member-row">'+avatar(m.full_name)+'<div><strong>'+esc(m.full_name)+'</strong><small>'+esc(m.email)+'</small></div><span class="badge">'+words(m.role)+'</span></div>').join('')+'</section></div>';
}
function reports(){
 const complete=state.tasks.filter(t=>t.status==='COMPLETED').length,total=state.tasks.length;
 return heading('See the bigger picture.','Live progress and practical signals, calculated from your team’s actual work.',button('export','','↓ Export task report'))+metrics()+
 '<div class="report-grid"><section class="panel"><h2>Work by stage</h2>'+statuses.map(s=>{const count=state.tasks.filter(t=>t.status===s).length;return '<div class="report-line"><strong>'+words(s)+'</strong><div class="progress"><span style="width:'+(total?count/total*100:0)+'%"></span></div><small>'+count+'</small></div>';}).join('')+'<div class="insight">'+(total?Math.round(complete/total*100)+'% of all tasks are complete. '+state.tasks.filter(overdue).length+' open tasks have passed their deadline.':'Create your first task to start tracking progress.')+'</div></section><section class="panel"><h2>Project completion</h2>'+state.projects.map(p=>'<div class="workload-row"><div class="workload-label"><span>'+esc(p.name)+'</span><small>'+progress(p.id)+'%</small></div><div class="progress"><span style="width:'+progress(p.id)+'%"></span></div></div>').join('')+'</section><section class="panel" style="grid-column:1/-1"><h2>Smart priorities</h2><p class="subtitle" style="margin-bottom:15px">A transparent ranking: priority weight × 10 + deadline urgency × 3. Earlier deadlines rise to the top; completed tasks are excluded.</p>'+taskTable(state.tasks.filter(t=>t.status!=='COMPLETED').sort((a,b)=>score(b)-score(a)).slice(0,8))+'</section></div>';
}
function users(){
 if(state.user.role!=='ADMIN')return empty('Administrator access required.');
 return heading('The right people. The right access.','Registered accounts and application roles. Project access is managed within each project.')+'<div class="panel table-wrap"><table><thead><tr><th>PERSON</th><th>EMAIL</th><th>APPLICATION ROLE</th><th>ACTION</th></tr></thead><tbody>'+state.users.map(u=>'<tr><td>'+esc(u.full_name)+'</td><td>'+esc(u.email)+'</td><td>'+words(u.role)+'</td><td>'+(u.id===state.user.id?'Your account':button('role',u.id,'Change role'))+'</td></tr>').join('')+'</tbody></table></div>';
}
function render(){
 const route=(location.hash||'#dashboard').slice(1),section=route.split('/')[0];
 const nav=[['dashboard','Overview','▦'],['projects','Projects','▣'],['tasks','Tasks','☷'],['team','Team','♧'],['reports','Reports','◴']];
 if(state.user.role==='ADMIN')nav.push(['users','Manage users','⚙']);
 $('#navigation').innerHTML=nav.map(([id,label,icon])=>'<a href="#'+id+'" class="nav-item '+(section===id?'active':'')+'"><span class="nav-icon">'+icon+'</span>'+label+'</a>').join('');
 $('#profile').textContent=initials(state.user.full_name);$('#profile').title=state.user.full_name;
 $('#breadcrumb').textContent=(nav.find(n=>n[0]===section)||['','Overview'])[1];
 $('#today').textContent=new Date().toLocaleDateString('en',{weekday:'short',month:'short',day:'numeric',year:'numeric'});
 main.innerHTML=section==='projects'?(route.includes('/')?projectDetail(route.split('/')[1]):projectList()):section==='tasks'?taskList():section==='team'?team():section==='reports'?reports():section==='users'?users():dashboard();
}
function openModal(title,body){$('#modal-content').innerHTML='<div class="modal-heading"><h2 id="modal-title">'+title+'</h2><button class="close" aria-label="Close dialog" data-action="close">×</button></div>'+body;if(!modal.open)modal.showModal();}
function form(title,action,body,hidden={},label='Save changes'){
 openModal(title,'<form id="edit-form" data-submit="'+action+'">'+Object.entries(hidden).map(([k,v])=>'<input type="hidden" name="'+k+'" value="'+esc(v)+'">').join('')+'<div class="form-grid">'+body+'</div><p class="form-error" role="alert"></p><div class="form-actions">'+button('close','','Cancel')+'<button class="btn primary" type="submit">'+label+'</button></div></form>');
}
function projectForm(id){
 const p=project(id)||{};
 form(id?'Edit project':'A new beginning',''+(id?'updateProject':'createProject'),
 field('Project name','name',p.name,'text',true,true)+textarea('Description','description',p.description)+field('Start date','start_date',p.start_date,'date')+field('Deadline','deadline',p.deadline,'date')+select('Project status','status',projectStatuses,p.status||'PLANNING',true),
 id?{project_id:id}:{},id?'Save project':'Create project');
 if(id)$('#edit-form .form-actions').insertAdjacentHTML('afterbegin','<button type="button" class="btn danger" data-action="deleteProject" data-id="'+id+'">Delete</button>');
}
function assigneeOptions(id){return [['','Unassigned'],...membersFor(id).map(m=>[m.user_id,m.full_name])];}
function taskForm(projectId,taskId){
 const t=state.tasks.find(t=>t.id===Number(taskId))||{},ps=state.projects.filter(p=>p.can_manage),id=t.project_id||Number(projectId)||ps[0]?.id;
 if(!id){notify('Create a project before adding tasks.',true);return;}
 form(taskId?'Edit task':'One step closer',taskId?'updateTask':'createTask',
 (taskId?'':select('Project','project_id',ps.map(p=>[p.id,p.name]),id,true))+
 field('Task title','title',t.title,'text',true,true)+textarea('Description','description',t.description)+select('Assignee','assigned_to',assigneeOptions(id),t.assigned_to||'')+select('Priority','priority',['LOW','MEDIUM','HIGH','URGENT'],t.priority||'MEDIUM')+field('Deadline','deadline',t.deadline,'date')+select('Status','status',statuses,t.status||'TODO'),
 taskId?{project_id:id,task_id:taskId}:{},taskId?'Save task':'Create task');
}
function taskDetail(id){
 const t=state.tasks.find(t=>t.id===Number(id));if(!t)return;
 const p=project(t.project_id),manage=p?.can_manage,update=manage||t.assigned_to===state.user.id;
 const comments=state.comments.filter(c=>c.task_id===t.id);
 openModal(esc(t.title),'<p class="subtitle">'+esc(t.project_name)+'</p><div class="task-meta"><span class="priority '+t.priority.toLowerCase()+'">'+words(t.priority)+'</span><span>◷ '+due(t.deadline)+'</span><span>'+esc(t.assignee_name||'Unassigned')+'</span>'+(update?'<select aria-label="Task status" id="task-status" data-task="'+t.id+'" data-project="'+t.project_id+'">'+statuses.map(s=>'<option value="'+s+'" '+(s===t.status?'selected':'')+'>'+words(s)+'</option>').join('')+'</select>':badge(t.status))+'</div><p class="task-description">'+esc(t.description||'No description added.')+'</p>'+(manage?'<div class="row-actions">'+button('editTask',t.id,'Edit task')+button('deleteTask',t.id,'Delete task','danger')+'</div>':'')+'<section class="comments"><h3>Conversation · '+comments.length+'</h3>'+comments.map(c=>'<div class="comment"><strong>'+esc(c.full_name)+'</strong><small>'+ago(c.created_at)+'</small><p>'+esc(c.body)+'</p></div>').join('')+'<form id="comment-form" class="comment-form"><textarea name="body" required maxlength="2000" aria-label="Write a comment" placeholder="Share an update or ask a question…"></textarea><button class="btn primary" type="submit">Post</button><input type="hidden" name="task_id" value="'+t.id+'"><input type="hidden" name="project_id" value="'+t.project_id+'"></form><p class="form-error" role="alert"></p></section>');
}
function invite(id){
 const existing=membersFor(id).map(m=>m.user_id),available=state.users.filter(u=>!existing.includes(u.id));
 if(!available.length){openModal('Your team is up to date',empty('All registered users are already on this project. New teammates can register using the sign-up page.'));return;}
 form('Bring someone on board','addMember',select('Registered teammate','user_id',available.map(u=>[u.id,u.full_name+' · '+u.email]),available[0].id,true),{project_id:id},'Add to project');
}
function exportReport(){ location.href=base+'/api/report.csv'; }
document.addEventListener('click',async e=>{
 const el=e.target.closest('[data-action]');if(!el)return;
 if(el.tagName==='BUTTON'&&!el.hasAttribute('type'))e.preventDefault();
 const id=el.dataset.id,action=el.dataset.action;
 try{
  if(action==='close')modal.close();
  else if(action==='openProject')location.hash='projects/'+id;
  else if(action==='newProject')projectForm();
  else if(action==='editProject')projectForm(id);
  else if(action==='newTask')taskForm(id);
  else if(action==='task')taskDetail(id);
  else if(action==='editTask')taskForm(null,id);
  else if(action==='invite')invite(id);
  else if(action==='board'||action==='list'){taskView=action;render();}
  else if(action==='export')exportReport();
  else if(action==='deleteProject'&&confirm('Permanently delete this project, its tasks and comments?'))await save(action,{project_id:id},'Project deleted');
  else if(action==='deleteTask'&&confirm('Permanently delete this task and its comments?')){const t=state.tasks.find(t=>t.id===Number(id));await save(action,{project_id:t.project_id,task_id:id},'Task deleted');}
  else if(action==='removeMember'&&confirm('Remove this teammate? Their tasks will become unassigned.'))await save(action,{project_id:el.dataset.project,user_id:id},'Team member removed');
  else if(action==='role'){const u=state.users.find(u=>u.id===Number(id));form('Update application role','updateRole','<p class="subtitle" style="grid-column:1/-1">'+esc(u.full_name)+'</p>'+select('Application role','role',['ADMIN','PROJECT_MANAGER','MEMBER'],u.role,true),{user_id:id});}
 }catch(error){notify(error.message,true);}
});
document.addEventListener('submit',async e=>{
 if(!['edit-form','comment-form'].includes(e.target.id))return;e.preventDefault();
 const f=e.target,submit=f.querySelector('[type=submit]'),values=Object.fromEntries(new FormData(f));
 submit.disabled=true;
 try{
  if(f.id==='comment-form'){await save('addComment',values,'Comment added',false);taskDetail(values.task_id);}
  else await save(f.dataset.submit,values);
 }catch(error){const errorEl=modal.querySelector('.form-error');if(errorEl)errorEl.textContent=error.message;else notify(error.message,true);}
 finally{submit.disabled=false;}
});
document.addEventListener('change',async e=>{
 const el=e.target;
 if(el.name==='project_id'&&el.closest('#edit-form')){const a=modal.querySelector('[name=assigned_to]');if(a)a.innerHTML=assigneeOptions(el.value).map(([v,n])=>'<option value="'+v+'">'+esc(n)+'</option>').join('');}
 if(el.id==='status-filter'){statusFilter=el.value;render();}
 if(el.id==='project-filter'){projectFilter=el.value;render();}
 if(el.id==='only-mine'){onlyMine=el.checked;render();}
 if(el.id==='task-status'){
  el.disabled=true;try{await save('setStatus',{project_id:el.dataset.project,task_id:el.dataset.task,status:el.value},'Task status updated',false);taskDetail(el.dataset.task);}catch(error){notify(error.message,true);taskDetail(el.dataset.task);}
 }
});
document.addEventListener('input',e=>{if(e.target.id==='search'){const start=e.target.selectionStart;search=e.target.value;render();$('#search').focus();$('#search').setSelectionRange(start,start);}});
window.addEventListener('hashchange',()=>{search='';statusFilter='';projectFilter='';render();});
$('#logout').addEventListener('click',async()=>{try{const r=await fetch(base+'/auth/logout',{method:'POST',headers:{'X-CSRF-Token':csrf}});if(!r.ok)throw Error('Could not sign out. Reload and try again.');location.href=base+'/login.jsp';}catch(e){notify(e.message,true);}});
$('#profile').addEventListener('click',()=>openModal('Your profile','<div class="profile-info"><div><small>Name</small>'+esc(state.user.full_name)+'</div><div><small>Email</small>'+esc(state.user.email)+'</div><div><small>Application role</small>'+words(state.user.role)+'</div></div><div class="form-actions"><button class="btn" data-action="signOut">Sign out</button></div>'));
modal.addEventListener('click',e=>{if(e.target.closest('[data-action=signOut]'))$('#logout').click();});
refresh().catch(error=>{main.innerHTML=empty('The workspace could not load. '+esc(error.message),'<button class="btn" onclick="location.reload()">Try again</button>');});
