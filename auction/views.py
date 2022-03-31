from django.contrib.auth import authenticate, login, logout
from django.db import IntegrityError
from django.http import HttpResponse, HttpResponseRedirect
from django.shortcuts import render
from django.urls import reverse
from django.utils import timezone
from django.contrib.auth.decorators import login_required
from .models import User, Listing, Watchlist, Bids, Comments
from decimal import Decimal

# python manage.py migrate --run-syncdb :sync and create table
# python manage.py makemigrations AppName :track the change in migration content.
#user1 Graph
#pass Graph06042536
#user2 Harry
#pass Potter1234
# ***logout before work on Session table

categories = ["Technology", "Kitchenware", "Houseware", "Books", "Vehicle", "Pet", "Others"]

def index(request):
    return render(request, "auctions/index.html", {
        'listings': Listing.objects.filter(status = True),
    })


def login_view(request):
    if request.method == "POST":

        # Attempt to sign user in
        username = request.POST["username"]
        password = request.POST["password"]
        user = authenticate(request, username=username, password=password)

        # Check if authentication successful
        if user is not None:
            login(request, user)
            return HttpResponseRedirect(reverse("index"))
        else:
            return render(request, "auctions/login.html", {
                "message": "Invalid username and/or password."
            })
    else:
        return render(request, "auctions/login.html")


def logout_view(request):
    logout(request)
    return HttpResponseRedirect(reverse("index"))


def register(request):
    if request.method == "POST":
        username = request.POST["username"]
        email = request.POST["email"]

        # Ensure password matches confirmation
        password = request.POST["password"]
        confirmation = request.POST["confirmation"]
        if password != confirmation:
            return render(request, "auctions/register.html", {
                "message": "Passwords must match."
            })

        # Attempt to create new user
        try:
            user = User.objects.create_user(username, email, password)
            user.save()
        except IntegrityError:
            return render(request, "auctions/register.html", {
                "message": "Username already taken."
            })
        login(request, user)
        return HttpResponseRedirect(reverse("index"))
    else:
        return render(request, "auctions/register.html")

#listing part

def create(request):
    if request.method == "POST":
        title = request.POST['title']
        description = request.POST['description']
        bid = request.POST['bid']
        url = request.POST['url']
        category = request.POST['category']
        listing = Listing(title = title, description = description, bid = bid, price = bid, img_url = url, 
                          category = category, user = User.objects.get(username = request.user.username))
        listing.save()
        return HttpResponseRedirect(reverse('create'))
    return render(request, "auctions/create.html", {
            'categories': categories
        })

@login_required
def listing(request, listing_id):
    #---- watchlist part ----
    if (request.method == "POST") & ("add" in request.POST):
        current_user = User.objects.get(username = request.user.username)
        current_listing = Listing.objects.get(pk = listing_id)
        w = Watchlist(user = current_user, item = current_listing)
        w.save()
        return HttpResponseRedirect(reverse('listing', kwargs={'listing_id': listing_id}))
    elif (request.method == "POST") & ("remove" in request.POST):
        #delete item from watchlist
        current_user = User.objects.get(username = request.user.username)
        current_listing = Listing.objects.get(pk = listing_id)
        Watchlist.objects.filter(user = current_user, item = current_listing).delete()
        return HttpResponseRedirect(reverse('listing', kwargs={'listing_id': listing_id}))
    #---- Bid part ----
    elif (request.method == "POST") & ("bid" in request.POST):
        current_bid = request.POST['bid']
        current_listing = Listing.objects.get(pk=listing_id)
        previuos_bids = Bids.objects.filter(item = current_listing)
        previous_bid_count = len(previuos_bids)
        latest_bid = current_listing.price
        start_bid = current_listing.bid
        current_user = User.objects.get(username = request.user.username)
        highest_bidder = ""
        if previous_bid_count != 0:
            if previuos_bids.latest('id').user.username == request.user.username:
                highest_bidder = "Your bid is the current bid"
            else:
                highest_bidder = previuos_bids.latest('id').user.username + "'s is the current bid."
        if (Decimal(current_bid) - latest_bid) < start_bid:
            #check watchlist
            in_watchlist = False
            current_listing = Listing.objects.get(pk = listing_id)
            current_user = User.objects.get(username = request.user.username)
            if Watchlist.objects.filter(user = current_user, item = current_listing).exists():
                #if item is already in watchlist the remove from watchlist button will appear instead
                in_watchlist = True
            #check comment
            have_comment = False
            if len(Comments.objects.all()) != 0:
                have_comment = True
            return render(request, "auctions/listing.html", {
                'listing': Listing.objects.get(pk=listing_id),
                'in_watchlist': in_watchlist,
                'message': f"{previous_bid_count} bid(s) so far. {highest_bidder} " + f"Your bid must be greater than or equal to {latest_bid+start_bid}.",
                'comment': have_comment,
                'comments': Comments.objects.all(),
                'current_user': request.user.username
             })
        else:
            b = Bids(item = current_listing, user = current_user, bid = current_bid)
            b.save()
            return HttpResponseRedirect(reverse('listing', kwargs={'listing_id': listing_id}))
    #close bid
    elif (request.method == "POST") & ("close" in request.POST):
        l = Listing.objects.get(pk = listing_id)
        l.status = False
        l.closed_time = timezone.now()
        l.save()
        return HttpResponseRedirect(reverse('listing', kwargs={'listing_id': listing_id}))
    #---- comment part ----
    #post comment
    elif (request.method == "POST") & ("comment" in request.POST):
        current_user = User.objects.get(username = request.user.username)
        current_listing = Listing.objects.get(pk=listing_id)
        c = Comments(comment = request.POST['comment'], user = current_user, item = current_listing)
        c.save()
        return HttpResponseRedirect(reverse('listing', kwargs={'listing_id': listing_id}))
    #delete comment
    elif (request.method == "POST") & ("delete_comment" in request.POST):
        Comments.objects.filter(pk = request.POST['comment_id']).delete()
        return HttpResponseRedirect(reverse('listing', kwargs={'listing_id': listing_id}))
    else:
        #---- Bid part ----
        # add check the latest price every time accessing this page. in case that the highest bid disappears.
        # Surprisingly, this check can be used to update the latest price.
        all_bid = Bids.objects.none()
        try:
            all_bid = Bids.objects.filter(item = Listing.objects.get(pk=listing_id))
        except Bids.DoesNotExist:
            all_bid = []
        current_listing = Listing.objects.get(pk=listing_id)
        #if the latest bid and the current price are not equal, update by strictly referring to Bids table.
        if (len(all_bid) != 0):
            if (all_bid.latest('id').bid != current_listing.price):
                l = Listing.objects.get(pk = listing_id)
                l.price = all_bid.latest('id').bid
                l.save()
        if len(all_bid) == 0:
            l = Listing.objects.get(pk = listing_id)
            l.price = l.bid
            l.save()
        #check whether current user is a person who bid highest or not
        highest_bidder = ""
        if len(all_bid) != 0:
            if all_bid.latest('id').user.username == request.user.username:
                highest_bidder = "Your bid is the current bid"
            else:
                highest_bidder = all_bid.latest('id').user.username + "'s is the current bid."
        #---- watchlist part ----
        #check whether the item is already in the watchlist of the user
        in_watchlist = False
        current_listing = Listing.objects.get(pk = listing_id)
        current_user = User.objects.get(username = request.user.username)
        if Watchlist.objects.filter(user = current_user, item = current_listing).exists():
            #if item is already in watchlist the remove from watchlist button will appear instead
            in_watchlist = True

        #---- comments part ----
        #check whether there are comments of this listing
        have_comment = False
        if len(Comments.objects.all()) != 0:
            have_comment = True
        #check whether listing is active or not
        if current_listing.status == True:
            return render(request, "auctions/listing.html", {
                'listing': Listing.objects.get(pk=listing_id),
                'in_watchlist': in_watchlist,
                'message': f"{len(all_bid)} bid(s) so far. {highest_bidder}",
                'comment': have_comment,
                'comments': Comments.objects.all(),
                'current_user': request.user.username
            })
        else:
            #this case is the listing is closed --> announce the winner
            message = ""
            print(len(all_bid))
            if len(all_bid) != 0:
                print("hi")
                winner = all_bid.latest('id').user.username
                latest_price = all_bid.latest('id').bid
                if winner == request.user.username:
                    winner = "You"
                message = f"{winner} won this auction at ${latest_price}."
            else:
                print("ho")
                message = "No winner of this auction."
            print(message)
            return render(request, "auctions/listing.html", {
                'listing': Listing.objects.get(pk=listing_id),
                'in_watchlist': in_watchlist,
                'message': message,
                'comment': have_comment,
                'comments': Comments.objects.all(),
                'current_user': request.user.username
            })


@login_required
def watchlist(request):
    current_user = User.objects.get(username = request.user.username)
    """
    if len(Watchlist.objects.filter(user = current_user)) == 0:
        return render(request, "auctions/watchlist.html", {
            'listings': None,
            'watchlist': False
        })
    else:
        w = Watchlist.objects.filter(user = current_user)
        watch_list = get_watchlist(w)
        return render(request, "auctions/watchlist.html", {
            'listings': watch_list,
            'watchlist': True
        })
    """
    w = Watchlist.objects.filter(user = current_user)
    watch_list = get_watchlist(w)
    return render(request, "auctions/watchlist.html", {
        'listings': watch_list,
    })

def category_main(request):
    all_listings = Listing.objects.all()
    current_categories = set()
    for listing in all_listings:
        current_categories.add(listing.category)
    return render(request, "auctions/category_main.html",{
        'categories': current_categories
    })

def category(request, listing_category):
    return render(request, "auctions/category.html", {
        'category': listing_category,
        'listings': Listing.objects.filter(category = listing_category)
    })

def closed_listings(request):
    return render(request, "auctions/closed_listings.html", {
        'listings': Listing.objects.filter(status = False),
    })

#supplement function

def get_watchlist(watchlist_query):
    # create an empty query set
    queryset = Listing.objects.none()
    try:
        for l in watchlist_query:
            # similar to append of list datatype, union is used in queryset datatype
            queryset = queryset.union(Listing.objects.filter(pk = l.item.id))
    except Listing.DoesNotExist:
        queryset = Listing.objects.none()
    return queryset