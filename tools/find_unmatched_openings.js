const fs=require('fs');
const path='C:/Users/Admin/Documents/GitHub/Project-Sem4-CP2496H07-Group5/EZMart_Supermarket_Management-war/web/resources/js/profile-address.js';
const s=fs.readFileSync(path,'utf8');
const lines=s.split(/\r?\n/);
let inS=false,inD=false,inB=false,esc=false;
const stack=[];
for(let i=0;i<lines.length;i++){
  const line=lines[i];
  for(let j=0;j<line.length;j++){
    const ch=line[j];
    if(esc){esc=false; continue;}
    if(inS){ if(ch==='\\') esc=true; else if(ch==="'") inS=false; continue; }
    if(inD){ if(ch==='\\') esc=true; else if(ch==='"') inD=false; continue; }
    if(inB){ if(ch==='\\') esc=true; else if(ch===String.fromCharCode(96)) inB=false; continue; }
    if(ch==="'") { inS=true; continue; }
    if(ch==='"') { inD=true; continue; }
    if(ch===String.fromCharCode(96)) { inB=true; continue; }
    if(ch==='('||ch==='{'||ch==='[') stack.push({c:ch,line:i+1});
    else if(ch===')'||ch==='}'||ch===']'){
      if(stack.length===0){ console.log('Unmatched closer',ch,'at',i+1); }
      else{
        const top=stack[stack.length-1];
        if((top.c==='('&&ch===')')||(top.c==='{'&&ch==='}')||(top.c==='['&&ch===']')) stack.pop();
        else { console.log('MISMATCH at',i+1,'expected close for',top.c,'but got',ch); stack.pop(); }
      }
    }
  }
}
if(inS||inD||inB) console.log('Unterminated string at EOF', {inS,inD,inB});
if(stack.length){
  console.log('UNMATCHED OPENINGS (bottom->top):');
  stack.forEach(it=>console.log(it.c,'line',it.line));
}else{
  console.log('No unmatched openings found.');
}
