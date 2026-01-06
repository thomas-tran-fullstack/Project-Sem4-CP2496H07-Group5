const fs=require('fs');
const p='EZMart_Supermarket_Management-war/web/resources/js/profile-address.js';
const s=fs.readFileSync(p,'utf8');
let i=0,len=s.length;
let inS=false,inD=false,inB=false,esc=false,inLineComment=false,inBlockComment=false;
let paren=0,brace=0,brack=0;
const stack=[];
for(i=0;i<len;i++){
  const ch=s[i];
  const nxt=s[i+1];
  if(inLineComment){ if(ch==='\n') inLineComment=false; continue; }
  if(inBlockComment){ if(ch==='*' && nxt==='/' ){ inBlockComment=false; i++; } continue; }
  if(esc){ esc=false; continue; }
  if(inS){ if(ch==='\\') esc=true; else if(ch==="'") inS=false; continue; }
  if(inD){ if(ch==='\\') esc=true; else if(ch==='"') inD=false; continue; }
  if(inB){ if(ch==='\\') esc=true; else if(ch==='`') inB=false; continue; }
  if(ch==='/' && nxt === '/') { inLineComment=true; i++; continue; }
  if(ch==='/' && nxt === '*') { inBlockComment=true; i++; continue; }
  if(ch==="'") { inS=true; continue; }
  if(ch==='"') { inD=true; continue; }
  if(ch==='`') { inB=true; continue; }
  if(ch==='('){ paren++; stack.push({c:'(',line:s.slice(0,i).split(/\r?\n/).length}); }
  else if(ch===')'){ paren--; if(stack.length && stack[stack.length-1].c==='(') stack.pop(); }
  else if(ch==='{'){ brace++; stack.push({c:'{',line:s.slice(0,i).split(/\r?\n/).length}); }
  else if(ch==='}'){ brace--; if(stack.length && stack[stack.length-1].c==='{') stack.pop(); }
  else if(ch==='['){ brack++; stack.push({c:'[',line:s.slice(0,i).split(/\r?\n/).length}); }
  else if(ch===']'){ brack--; if(stack.length && stack[stack.length-1].c==='[') stack.pop(); }
  if(paren<0||brace<0||brack<0){
    console.log('NEGATIVE at index',i,'char',ch,'line',s.slice(0,i).split(/\r?\n/).length,'paren',paren,'brace',brace,'brack',brack);
  }
}
console.log('FINAL paren',paren,'brace',brace,'brack',brack);
if(stack.length){
  console.log('REMAINING OPENINGS (bottom->top):');
  stack.forEach(it=>console.log(it.c,'line',it.line));
}
// print last 40 lines
const lines=s.split(/\r?\n/);
console.log('TOTAL LINES',lines.length);for(let j=Math.max(0,lines.length-40);j<lines.length;j++) console.log((j+1)+': '+lines[j]);
