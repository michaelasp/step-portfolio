// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * Adds a random greeting to the page.
 */
function addRandomGreeting() {
  const greetings =
      ['I am Russian and Peruvian', 'I\'ve placed 3rd in a mountain bike race', 
        'I was born in California', 'I love cooking', 'I am a dual citizen, (US/Russia)'];

  // Pick a random greeting.
  const greeting = greetings[Math.floor(Math.random() * greetings.length)];

  // Add it to the page.
  const greetingContainer = document.getElementById('fact-container');
  greetingContainer.innerText = greeting;
}

function fetchFact(){
    fetch("/data").then(response => response.text()).then((fact) => {
        document.getElementById("fact-container").innerText = fact;
    });
}

function createCommentDiv(comment) {
    let commentDiv = document.createElement("div");
    let mediaBody = document.createElement("div");
    let user = document.createElement("h4");
    let commentText = document.createElement("p");

    commentDiv.className = "media well";
    mediaBody.className = "media-body";
    user.className = "media-heading";
    user.innerText = comment.name;
    commentText.innerText = comment.text;

    commentDiv.append(mediaBody);
    mediaBody.append(user);
    mediaBody.append(commentText);

    return commentDiv;
}   

function fetchComments(amount) {
    console.log(amount);
    let commentElem = document.getElementById("comments");
    commentElem.innerHTML = "";
    fetch(`/data?amount=${amount}`).then(response => response.json()).then((comments) => {
        console.log(comments);
        comments.forEach(comment => {
            commentElem.append(createCommentDiv(comment));
        });
    });
}