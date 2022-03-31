from django.shortcuts import render
from django.http import HttpResponseRedirect
from django.urls import reverse
from django import forms
from . import util
import markdown
import random

md = markdown.Markdown()

class TitleForm(forms.Form):
    title = forms.CharField(label="Title")

class ContentForm(forms.Form):
    content = forms.CharField(label="", widget=forms.Textarea(attrs={'placeholder': 'Type here'}))

def index(request):
    """
        Argument: request
        Functionality:
            1. return the rendered page of "encyclopedia/index.html" which contains the list of entries you have
               in your database when access the url:/wiki/ directly
            2. redirect to the entry or see the search results when we type the keyword in search field
    """
    if (request.method == "GET") & (request.GET.get("q") != None):
        """
            when we type in the search field the value of q is not equal to None so we use this condition
            to distinguish the redirect access from search field and the common access from typing urls
        """
        search_redirect = search(request)
        return search_redirect
    else:
        random_entry = rand()
        return render(request, "encyclopedia/index.html", {
            "entries": util.list_entries(),
            "random_entry": random_entry
        })    

def entry(request, entry):
    """
        Arguments:
            1. request
            2. entry -- the name of entry according to the url:/wiki/{entry}
        Functioanlity:
            1. return the rendered page of "encyclopedia/entry.html" which contains the content of entry's page
               which is converted from .md file in the database
            2. if the entry's name doesn't exist, dispaly the error page.
            3. redirect to the entry or see the search results when we type the keyword in search field
    """
    if (request.method == "GET") & (request.GET.get("q") != None):
        """
            In the case of entry's page acccess, we use the condition to distinguish the direct access from
            typing url:/wiki/{entry's name} and the redirect access from the search field.
        """
        search_redirect = search(request)
        return search_redirect
    else:
        if entry in util.list_entries():
            random_entry = rand()
            return render(request, "encyclopedia/entry.html", {
                "title": entry,
                "content" : md.convert(util.get_entry(entry)),
                "random_entry": random_entry
            })
        else:
            random_entry = rand()
            return render(request, "encyclopedia/error.html", {
                "message": f"The requseted page - {entry} - was not found.",
                "random_entry": random_entry
            })

def create(request):
    """
        Argument: request
        Variables:
            t_form -- form for title
            c_form -- form for content
        Functionality:
            1. return the rendered page of "encyclopedia/create.html" which contains the forms for title and content,
               and also the button for saving the data.
            2. allow user to create and save the content in {entry's name}.md format. if the title is already exist, 
               display the error message. Otherwise, save the created page.
            3. redirect to the entry or see the search results when we type the keyword in search field
    """
    if request.method == "POST":
        t_form = TitleForm(request.POST)
        c_form = ContentForm(request.POST)
        if t_form.is_valid() & c_form.is_valid():
            title_data = t_form.cleaned_data["title"]
            content_data = c_form.cleaned_data["content"]
            entries = util.list_entries()
            if title_data in entries:
                message = f"The title '{title_data}' is already exist."
                random_entry = rand()
                return render(request, 'encyclopedia/error.html', {
                    "message": message,
                    "random_entry": random_entry
                })
            else:
                util.save_entry(title_data, content_data)
                random_entry = rand()
                return render(request, "encyclopedia/entry.html", {
                    "title": title_data,
                    "content" : md.convert(util.get_entry(title_data)),
                    "random_entry": random_entry
                })
        else:
            rand_entry = rand()
            return render(request, "encyclopedia/create.html",{
                "title_form": t_form,
                "content_form": c_form,
                "random_entry": rand_entry
             })
    elif (request.method == "GET") & (request.GET.get("q") != None):
        search_redirect = search(request)
        return search_redirect
    else:
        rand_entry = rand()
        return render(request, "encyclopedia/create.html",{
            "title_form": TitleForm(),
            "content_form": ContentForm(),
            "random_entry": rand_entry
        })

def edit(request, entry):
    """
        Argument: request
        Functionality:
            1. return the rendered page of "encyclopedia/edit.html" which contains the forms for title and content
               which pre-populated the markdown content you currently have, and also the button for saving the changes
               you make.
            2. allow user to edit and save the changes you make. if the title is already exist, 
               display the error message. Otherwise, save the created page.
            3. redirect to the entry or see the search results when we type the keyword in search field
    """
    if request.method == "POST":
        c_form = ContentForm(request.POST)
        if c_form.is_valid():
            # in the case that we use form, don't forget to write if-else to support both form is valid
            # and form is invalid. Otherwise, django will throw an exception.
            content_data = c_form.cleaned_data["content"]
            util.save_entry(entry, content_data)
            random_entry = rand()
            return render(request, 'encyclopedia/entry.html',{
                "title": entry,
                "content" : md.convert(util.get_entry(entry)),
                "random_entry": random_entry
            })
        else: 
            rand_entry = rand()
            return render(request, "encyclopedia/edit.html",{
                "title": entry,
                "content_form": c_form,
                "random_entry": rand_entry
            })
    elif (request.method == "GET") & (request.GET.get("q") != None):
        search_redirect = search(request)
        return search_redirect
    else:
        c_form = ContentForm({'content': util.get_entry(entry)})
        rand_entry = rand()
        return render(request, "encyclopedia/edit.html",{
            "title": entry,
            "content_form": c_form,
            "random_entry": rand_entry
        })


# supplementary functions

def word_finding(entries, key):
    """
        Arguments:
            1. entries -- list of all entries you currently have
            2. key -- the keyword you typed in the search field
        return:
            the list of all entries which have the key as a part or exactly match to the key, case sensitive
            (key is subset/part of the entry's name)
    """
    result = []
    for entry in entries:
        if key in entry:
            result.append(entry)
    return result

def rand():
    """
        return a randomly selected entry for random search link
    """
    entries = util.list_entries()
    index = random.randrange(0,len(entries),1)
    random_entry = entries[index]
    return random_entry

def search(request):
    """
        Argument: request -- contain the value of request.GET
        Funtionality:
            1. if the keyword is exactly the same as any entry's name you have, redirect to that entry's page
            2. if the keyword is a subset of entry's names(not exactly matched), return a list of entry's name that match the
               condition
            3. if the keyword don't match any of entries you have, display an error page.
    """
    entry_req = request.GET.get("q")
    entries = util.list_entries()
    if entry_req in entries:
        return HttpResponseRedirect(reverse('entry', kwargs={'entry': entry_req}))
    else:
        search_result = word_finding(entries, entry_req)
        message = None
        check = True
        if len(search_result) == 0:
            message = f"Your search keyword - {entry_req} - did not match any documents."
            check = False
        if entry_req == "":
            return HttpResponseRedirect(reverse("index"))
        else:
            return render(request, "encyclopedia/search.html",{
                "entries": search_result,
                "message": message,
                "exist": check,
            })