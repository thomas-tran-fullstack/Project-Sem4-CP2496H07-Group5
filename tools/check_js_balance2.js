const fs = require('fs');
const path = 'C:/Users/Admin/Documents/GitHub/Project-Sem4-CP2496H07-Group5/EZMart_Supermarket_Management-war/web/resources/js/profile-address.js';
const s = fs.readFileSync(path, 'utf8');
let inSingle=false,inDouble=false,inBack=false,esc=false;
let paren=0,brace=0,brack=0;
for(let i=0;i<s.length;i++){
  const ch=s[i];
  if(esc){esc=false;continue}
  if(inSingle){ if(ch==='\\') esc=true; else if(ch==="'") inSingle=false; continue }
  if(inDouble){ if(ch==='\\') esc=true; else if(ch==='"') inDouble=false; continue }
  if(inBack){ if(ch==='\\') esc=true; else if(ch===String.fromCharCode(96)) inBack=false; continue }
  if(ch==="'") { inSingle=true; continue }
  if(ch==='"') { inDouble=true; continue }
  if(ch===String.fromCharCode(96)) { inBack=true; continue }
  if(ch==='(') paren++;
  else if(ch===')') paren--;
  else if(ch==='{') brace++;
  else if(ch==='}') brace--;
  else if(ch==='[') brack++;
  else if(ch===']') brack--;
}
console.log('NET -> paren:',paren,'brace:',brace,'bracket:',brack);
console.log('inSingle',inSingle,'inDouble',inDouble,'inBack',inBack);
